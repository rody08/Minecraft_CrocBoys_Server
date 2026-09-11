[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory = $true)]
    [ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })]
    [string] $JarPath,

    [string] $HostName,

    [int] $Port,

    [string] $UserName,

    [string] $RemoteDirectory,

    [string] $ConnectionFile = (Join-Path $PSScriptRoot '..\.pebblehost\connection.psd1'),

    [switch] $DryRun
)

$ErrorActionPreference = 'Stop'

if (Test-Path -LiteralPath $ConnectionFile -PathType Leaf) {
    $connection = Import-PowerShellDataFile -LiteralPath $ConnectionFile
    if (-not $HostName) { $HostName = $connection.HostName }
    if (-not $Port) { $Port = $connection.Port }
    if (-not $UserName) { $UserName = $connection.UserName }
    if (-not $RemoteDirectory) { $RemoteDirectory = $connection.RemoteDirectory }
}

if (-not $HostName -or -not $UserName -or $Port -lt 1 -or $Port -gt 65535) {
    throw 'Provide HostName, Port, and UserName or create .pebblehost/connection.psd1.'
}
if (-not $RemoteDirectory) {
    $RemoteDirectory = '/plugins/'
}

$resolvedJar = (Resolve-Path -LiteralPath $JarPath).Path
if ([IO.Path]::GetExtension($resolvedJar) -ne '.jar') {
    throw "JarPath must point to a .jar file: $resolvedJar"
}

$scp = Get-Command scp -ErrorAction SilentlyContinue
if (-not $scp) {
    throw 'OpenSSH scp was not found. Enable the Windows OpenSSH Client or use the PebbleHost File Manager.'
}

$remotePath = $RemoteDirectory.TrimEnd('/') + '/' + [IO.Path]::GetFileName($resolvedJar)
$destination = "${UserName}@${HostName}:$remotePath"

Write-Host "Local:  $resolvedJar"
Write-Host "Remote: ${HostName}:$remotePath"
Write-Host "Port:   $Port"

if ($DryRun) {
    Write-Host 'Dry run only; nothing was uploaded.'
    return
}

if ($PSCmdlet.ShouldProcess($destination, 'Upload plugin JAR over SCP')) {
    & $scp.Source -P $Port -- $resolvedJar $destination
    if ($LASTEXITCODE -ne 0) {
        throw "SCP upload failed with exit code $LASTEXITCODE."
    }

    Write-Host 'Upload completed. Check the PebbleHost console during the next server start/restart.'
}
