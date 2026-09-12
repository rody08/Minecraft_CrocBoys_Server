[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $SourceConfig,

    [Parameter(Mandatory = $true)]
    [string] $OutputConfig
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repositoryRoot '.env'
$tunnelPath = Join-Path $repositoryRoot '.pebblehost\ollama-tunnel-url.txt'

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
$model = $settings['NYX_OLLAMA_MODEL']
if ([string]::IsNullOrWhiteSpace($model)) {
    $model = $settings['BIGOSCIE_OLLAMA_MODEL']
}
$tunnelUrl = (Get-Content -Raw -LiteralPath $tunnelPath).Trim().TrimEnd('/')
if ([string]::IsNullOrWhiteSpace($token) -or $token.Length -lt 32) {
    throw 'The local relay token is missing or too short.'
}
if ([string]::IsNullOrWhiteSpace($model)) {
    throw 'NYX_OLLAMA_MODEL is missing from .env.'
}
if ($tunnelUrl -notmatch '^https://[a-z0-9-]+\.trycloudflare\.com$') {
    throw 'The current Quick Tunnel URL is missing or invalid.'
}

$source = Get-Content -LiteralPath $SourceConfig
$sourceVersion = @($source | Where-Object { $_ -match '^config-version:' })
if ($sourceVersion.Count -gt 1) {
    throw "Expected at most one config-version setting; found $($sourceVersion.Count)."
}
$output = [Collections.Generic.List[string]]::new()
$output.Add($(if ($sourceVersion.Count -eq 1) { $sourceVersion[0] } else { 'config-version: "0.2.2"' }))
$output.Add('')
$inAi = $false
$counts = @{ enabled = 0; endpoint = 0; model = 0; apiKey = 0 }

foreach ($line in $source) {
    if ($line -match '^config-version:') { continue }
    if ($line -match '^ai:\s*$') {
        $inAi = $true
        $output.Add($line)
        $output.Add('  provider: "ollama"')
        continue
    }
    if ($inAi -and $line -match '^\S') { $inAi = $false }

    if ($inAi -and $line -match '^  provider:') { continue }
    if ($inAi -and $line -match '^  enabled:') {
        $output.Add('  enabled: true')
        $counts.enabled++
        continue
    }
    if ($inAi -and $line -match '^  endpoint:') {
        $output.Add("  endpoint: `"$tunnelUrl/v1/responses`"")
        $counts.endpoint++
        continue
    }
    if ($inAi -and $line -match '^  model:') {
        $output.Add("  model: `"$model`"")
        $counts.model++
        continue
    }
    if ($inAi -and $line -match '^  api-key:') {
        $output.Add("  api-key: `"$token`"")
        $counts.apiKey++
        continue
    }
    $output.Add($line)
}

foreach ($key in @('enabled', 'endpoint', 'model', 'apiKey')) {
    if ($counts[$key] -ne 1) {
        throw "Expected exactly one ai.$key setting; found $($counts[$key])."
    }
}

Set-Content -LiteralPath $OutputConfig -Value $output -Encoding utf8
Write-Host "Prepared Ollama configuration: $OutputConfig"
