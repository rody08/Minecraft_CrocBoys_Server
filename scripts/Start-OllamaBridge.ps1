[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$localState = Join-Path $repositoryRoot '.pebblehost'
$envPath = Join-Path $repositoryRoot '.env'
$python = Join-Path $localState 'python\Scripts\python.exe'
$relayScript = Join-Path $PSScriptRoot 'ollama_relay.py'
$cloudflared = 'C:\Program Files (x86)\cloudflared\cloudflared.exe'

foreach ($required in @($envPath, $python, $relayScript, $cloudflared)) {
    if (-not (Test-Path -LiteralPath $required -PathType Leaf)) {
        throw "Required bridge component is missing: $required"
    }
}

function Read-DotEnv([string] $Path) {
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*#' -or $line -notmatch '=') { continue }
        $key, $value = $line -split '=', 2
        $values[$key.Trim()] = $value.Trim().Trim('"').Trim("'")
    }
    return $values
}

$settings = Read-DotEnv $envPath
$token = $settings['BIGOSCIE_OLLAMA_RELAY_TOKEN']
$port = [int]$settings['BIGOSCIE_OLLAMA_RELAY_PORT']
$model = $settings['BIGOSCIE_OLLAMA_MODEL']
if ([string]::IsNullOrWhiteSpace($token) -or $token.Length -lt 32) {
    throw 'Run scripts/Setup-OllamaBridge.ps1 to generate the relay token.'
}

$null = Invoke-RestMethod -Uri 'http://127.0.0.1:11434/api/version' -TimeoutSec 5

$relayOut = Join-Path $localState 'ollama-relay.out.log'
$relayError = Join-Path $localState 'ollama-relay.error.log'
$relayProcess = Start-Process `
    -FilePath $python `
    -ArgumentList @($relayScript, '--env-file', $envPath) `
    -WindowStyle Hidden `
    -RedirectStandardOutput $relayOut `
    -RedirectStandardError $relayError `
    -PassThru
Set-Content -LiteralPath (Join-Path $localState 'ollama-relay.pid') -Value $relayProcess.Id

$headers = @{ Authorization = "Bearer $token" }
$relayReady = $false
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:$port/health" -Headers $headers -TimeoutSec 2
        if ($health.status -eq 'ok') { $relayReady = $true; break }
    } catch {
        Start-Sleep -Milliseconds 250
    }
}
if (-not $relayReady) {
    Stop-Process -Id $relayProcess.Id -ErrorAction SilentlyContinue
    throw "The local relay did not start. Check $relayError"
}

$tunnelOut = Join-Path $localState 'cloudflared.out.log'
$tunnelError = Join-Path $localState 'cloudflared.error.log'
$tunnelProcess = Start-Process `
    -FilePath $cloudflared `
    -ArgumentList @('tunnel', '--url', "http://127.0.0.1:$port") `
    -WindowStyle Hidden `
    -RedirectStandardOutput $tunnelOut `
    -RedirectStandardError $tunnelError `
    -PassThru
Set-Content -LiteralPath (Join-Path $localState 'cloudflared.pid') -Value $tunnelProcess.Id

$tunnelUrl = $null
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    Start-Sleep -Milliseconds 500
    $logs = (Get-Content -Raw -LiteralPath $tunnelOut -ErrorAction SilentlyContinue) +
        (Get-Content -Raw -LiteralPath $tunnelError -ErrorAction SilentlyContinue)
    $match = [regex]::Match($logs, 'https://[a-z0-9-]+\.trycloudflare\.com')
    if ($match.Success) { $tunnelUrl = $match.Value; break }
    if ($tunnelProcess.HasExited) { break }
}
if (-not $tunnelUrl) {
    Stop-Process -Id $tunnelProcess.Id -ErrorAction SilentlyContinue
    Stop-Process -Id $relayProcess.Id -ErrorAction SilentlyContinue
    throw "Cloudflare Tunnel did not start. Check $tunnelError"
}

$endpoint = $tunnelUrl + '/v1/responses'
Set-Content -LiteralPath (Join-Path $localState 'ollama-tunnel-url.txt') -Value $tunnelUrl

$testBody = @{
    model = $model
    instructions = 'Reply with exactly: bridge ready'
    input = 'Connectivity test'
    max_output_tokens = 12
} | ConvertTo-Json
$test = $null
$lastTestError = $null
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    try {
        $test = Invoke-RestMethod `
            -Method Post `
            -Uri $endpoint `
            -Headers $headers `
            -ContentType 'application/json' `
            -Body $testBody `
            -TimeoutSec 90
        if ($test.status -eq 'completed') { break }
        $lastTestError = "The public bridge returned status: $($test.status)"
    } catch {
        $lastTestError = $_.Exception.Message
    }
    Start-Sleep -Seconds 2
}
if ($null -eq $test -or $test.status -ne 'completed') {
    Stop-Process -Id $tunnelProcess.Id -ErrorAction SilentlyContinue
    Stop-Process -Id $relayProcess.Id -ErrorAction SilentlyContinue
    throw "The public bridge test failed after retries: $lastTestError"
}

Write-Host "Ollama bridge is ready for $model."
Write-Host "Endpoint: $endpoint"
Write-Host 'The relay token remains only in the Git-ignored .env file.'
