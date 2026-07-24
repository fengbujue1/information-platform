[CmdletBinding()]
param(
    [string]$ComposeDirectory
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ComposeDirectory)) {
    $ComposeDirectory = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
}

function Assert-Task004A {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw "TASK-004A validation failed: $Message"
    }
}

$composeFile = Join-Path $ComposeDirectory "docker-compose.yml"
$exampleEnvFile = Join-Path $ComposeDirectory ".env.example"
$initFile = Join-Path $ComposeDirectory "mysql/init/01-create-databases.sh"

Assert-Task004A (Test-Path -LiteralPath $composeFile) "docker-compose.yml is missing"
Assert-Task004A (Test-Path -LiteralPath $exampleEnvFile) ".env.example is missing"
Assert-Task004A (Test-Path -LiteralPath $initFile) "initialization script is missing"

$composeJson = & docker compose `
    --env-file $exampleEnvFile `
    --file $composeFile `
    config `
    --format json

Assert-Task004A ($LASTEXITCODE -eq 0) "docker compose config failed"

$config = $composeJson | ConvertFrom-Json
$mysql = $config.services.mysql

Assert-Task004A ($null -ne $mysql) "mysql service is missing"
Assert-Task004A ($mysql.image -eq "mysql:8.4.10") "image must be pinned to mysql:8.4.10"
Assert-Task004A ($mysql.restart -eq "unless-stopped") "restart policy must be unless-stopped"
Assert-Task004A ($null -ne $mysql.healthcheck) "healthcheck is missing"

$ports = @($mysql.ports)
Assert-Task004A ($ports.Count -eq 1) "exactly one MySQL port mapping is required"
Assert-Task004A ($ports[0].host_ip -eq "127.0.0.1") "MySQL port must bind to 127.0.0.1"
Assert-Task004A ([int]$ports[0].target -eq 3306) "container port must be 3306"

$dataMount = @(
    @($mysql.volumes) | Where-Object {
        $_.type -eq "volume" -and $_.target -eq "/var/lib/mysql"
    }
)
$initMount = @(
    @($mysql.volumes) | Where-Object {
        $_.type -eq "bind" -and
        $_.target -eq "/docker-entrypoint-initdb.d/01-create-databases.sh" -and
        $_.read_only
    }
)

Assert-Task004A ($dataMount.Count -eq 1) "named data volume is missing"
Assert-Task004A ($initMount.Count -eq 1) "read-only initialization mount is missing"
Assert-Task004A (
    -not ($mysql.environment.PSObject.Properties.Name -contains "MYSQL_ROOT_HOST")
) "MYSQL_ROOT_HOST must not be configured"

$composeText = Get-Content -Raw -Encoding utf8 $composeFile
$initText = Get-Content -Raw -Encoding utf8 $initFile
$exampleEnvText = Get-Content -Raw -Encoding utf8 $exampleEnvFile

Assert-Task004A (-not $composeText.Contains("mysql:latest")) "latest image tag is forbidden"
Assert-Task004A (-not $composeText.Contains("0.0.0.0:")) "public port binding is forbidden"
Assert-Task004A ($exampleEnvText.Contains("CHANGE_ME_ROOT")) "root password placeholder is missing"
Assert-Task004A ($exampleEnvText.Contains("CHANGE_ME_DEV")) "development password placeholder is missing"
Assert-Task004A ($exampleEnvText.Contains("CHANGE_ME_TEST")) "test password placeholder is missing"

foreach ($businessTable in @(
    "information_item",
    "job_information",
    "information_snapshot"
)) {
    Assert-Task004A (
        -not $initText.Contains($businessTable)
    ) "initialization script must not create business table $businessTable"
}

& docker compose `
    --env-file $exampleEnvFile `
    --file $composeFile `
    config `
    --quiet

Assert-Task004A ($LASTEXITCODE -eq 0) "docker compose config --quiet failed"

Write-Host "TASK-004A Compose validation passed."
