[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$environmentPath = Join-Path $repositoryRoot '.pebblehost\python'
$bundledPython = 'C:\Users\rsoto\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'

if (-not (Test-Path -LiteralPath $bundledPython -PathType Leaf)) {
    throw 'The bundled Codex Python runtime was not found.'
}

if (-not (Test-Path -LiteralPath (Join-Path $environmentPath 'Scripts\python.exe'))) {
    & $bundledPython -m venv $environmentPath
}

$environmentPython = Join-Path $environmentPath 'Scripts\python.exe'
& $environmentPython -m pip install --upgrade paramiko

if ($LASTEXITCODE -ne 0) {
    throw "SFTP dependency installation failed with exit code $LASTEXITCODE."
}

Write-Host 'SFTP tools are ready.'
