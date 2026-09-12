[CmdletBinding()]
param(
    [ValidateSet('Screen', 'Full')] [string] $Suite = 'Screen',
    [string] $RunDir,
    [string[]] $Models,
    [string[]] $Cases,
    [ValidateRange(1, 20)] [int] $Repetitions = 0,
    [ValidateSet('Stable', 'Recommended', 'Both')] [string] $Sampling = 'Stable',
    [ValidateSet(4096, 8192)] [int] $Context = 4096,
    [ValidateSet('Idle', 'Gaming')] [string] $Workload = 'Idle',
    [switch] $PullMissing,
    [switch] $Background,
    [switch] $ReportOnly
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$python = Join-Path $repositoryRoot '.pebblehost/python/Scripts/python.exe'
if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
    $pythonCommand = Get-Command python -ErrorAction SilentlyContinue
    if ($null -eq $pythonCommand) { throw 'Python 3.10+ is required. Install Python or run the existing SFTP tools setup.' }
    $python = $pythonCommand.Source
}

$benchmarkArgs = @((Join-Path $PSScriptRoot 'nyx_benchmark.py'), '--suite', $Suite.ToLowerInvariant(),
    '--sampling', $Sampling.ToLowerInvariant(), '--context', "$Context", '--workload', $Workload.ToLowerInvariant())
if ($RunDir) { $benchmarkArgs += @('--run-dir', $RunDir) }
if ($Models) { $benchmarkArgs += @('--models') + $Models }
if ($Cases) { $benchmarkArgs += @('--cases') + $Cases }
if ($Repetitions -gt 0) { $benchmarkArgs += @('--repetitions', "$Repetitions") }
if ($PullMissing) { $benchmarkArgs += '--pull-missing' }
if ($Background) { $benchmarkArgs += '--background' }
if ($ReportOnly) { $benchmarkArgs += '--report-only' }

& $python @benchmarkArgs
if ($LASTEXITCODE -ne 0) { throw "Nyx benchmark exited with code $LASTEXITCODE. See the run folder or error above." }
