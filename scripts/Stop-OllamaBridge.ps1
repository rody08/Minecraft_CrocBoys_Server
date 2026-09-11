[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$localState = Join-Path $repositoryRoot '.pebblehost'

foreach ($name in @('cloudflared', 'ollama-relay')) {
    $pidPath = Join-Path $localState "$name.pid"
    if (-not (Test-Path -LiteralPath $pidPath -PathType Leaf)) { continue }
    $processId = [int](Get-Content -Raw -LiteralPath $pidPath).Trim()
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        $expected = if ($name -eq 'cloudflared') { 'cloudflared' } else { 'python' }
        if ($process.ProcessName -notlike "$expected*") {
            throw "PID $processId belongs to $($process.ProcessName), not $expected; refusing to stop it."
        }
        Stop-Process -Id $processId
    }
    Remove-Item -LiteralPath $pidPath
}

Write-Host 'The CrocBoys Ollama relay and tunnel are stopped.'
