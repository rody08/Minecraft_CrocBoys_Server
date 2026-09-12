[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repositoryRoot '.env'

if (-not (Test-Path -LiteralPath $envPath -PathType Leaf)) {
    Copy-Item -LiteralPath (Join-Path $repositoryRoot '.env.example') -Destination $envPath
}

$content = Get-Content -Raw -LiteralPath $envPath
if ($content -notmatch '(?m)^NYX_OLLAMA_MODEL=') {
    $legacyModel = [regex]::Match($content, '(?m)^BIGOSCIE_OLLAMA_MODEL=(.+)$')
    $modelValue = if ($legacyModel.Success) { $legacyModel.Groups[1].Value.Trim() } else { 'hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M' }
    $content += "`r`nNYX_OLLAMA_MODEL=$modelValue"
}
if ($content -notmatch '(?m)^BIGOSCIE_OLLAMA_RELAY_PORT=') {
    $content += "`r`nBIGOSCIE_OLLAMA_RELAY_PORT=11435"
}

$tokenMatch = [regex]::Match($content, '(?m)^BIGOSCIE_OLLAMA_RELAY_TOKEN=(.*)$')
if (-not $tokenMatch.Success -or [string]::IsNullOrWhiteSpace($tokenMatch.Groups[1].Value)) {
    $bytes = [byte[]]::new(32)
    [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    $token = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
    if ($tokenMatch.Success) {
        $content = [regex]::Replace(
            $content,
            '(?m)^BIGOSCIE_OLLAMA_RELAY_TOKEN=.*$',
            "BIGOSCIE_OLLAMA_RELAY_TOKEN=$token"
        )
    } else {
        $content += "`r`nBIGOSCIE_OLLAMA_RELAY_TOKEN=$token"
    }
}
Set-Content -LiteralPath $envPath -Value $content -NoNewline

$configuredModel = [regex]::Match($content, '(?m)^NYX_OLLAMA_MODEL=(.+)$').Groups[1].Value.Trim()
if ([string]::IsNullOrWhiteSpace($configuredModel)) {
    throw 'NYX_OLLAMA_MODEL is missing from .env.'
}

$version = Invoke-RestMethod -Uri 'http://127.0.0.1:11434/api/version' -TimeoutSec 5
try {
    $model = Invoke-RestMethod `
        -Method Post `
        -Uri 'http://127.0.0.1:11434/api/show' `
        -ContentType 'application/json' `
        -Body (@{ model = $configuredModel } | ConvertTo-Json) `
        -TimeoutSec 10
} catch {
    throw "The configured Nyx model '$configuredModel' is not installed."
}

Write-Host "Ollama $($version.version) and $configuredModel are ready."
Write-Host 'A strong relay token is stored only in the Git-ignored .env file.'
