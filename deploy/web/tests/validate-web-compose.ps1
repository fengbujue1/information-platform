param(
    [string]$ComposeDirectory = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path,
    [switch]$StaticOnly
)

$ErrorActionPreference = "Stop"

function Assert-Contains {
    param(
        [string]$Text,
        [string]$Pattern,
        [string]$Message
    )

    if ($Text -notmatch $Pattern) {
        throw $Message
    }
}

function Assert-NotContains {
    param(
        [string]$Text,
        [string]$Pattern,
        [string]$Message
    )

    if ($Text -match $Pattern) {
        throw $Message
    }
}

$composePath = Join-Path $ComposeDirectory "docker-compose.yml"
$envExamplePath = Join-Path $ComposeDirectory ".env.example"
$dockerfilePath = Join-Path $ComposeDirectory "web\Dockerfile"
$nginxPath = Join-Path $ComposeDirectory "web\nginx\default.conf.template"

foreach ($path in @($composePath, $envExamplePath, $dockerfilePath, $nginxPath)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required web deployment file is missing: $path"
    }
}

$composeText = Get-Content -LiteralPath $composePath -Raw
$dockerfileText = Get-Content -LiteralPath $dockerfilePath -Raw
$nginxText = Get-Content -LiteralPath $nginxPath -Raw

Assert-Contains $composeText 'profiles:\s*\r?\n\s*-\s*web' "Web service must be opt-in through the web profile."
Assert-Contains $composeText '127\.0\.0\.1:\$\{WEB_HOST_PORT:-18080\}:8080' "Web port must bind to loopback only."
Assert-NotContains $composeText '0\.0\.0\.0:\$\{WEB_HOST_PORT' "Web port must not bind to every interface."
Assert-Contains $dockerfileText 'FROM node:24\.18\.0-alpine3\.24 AS build' "Node build image must be pinned."
Assert-Contains $dockerfileText 'FROM nginx:1\.30\.4-alpine3\.24' "Nginx runtime image must be pinned."
Assert-Contains $dockerfileText 'RUN npm ci' "Frontend image must use npm ci."
Assert-Contains $dockerfileText 'RUN npm run build' "Frontend image must run the production build."
Assert-Contains $nginxText 'try_files \$uri \$uri/ /index\.html;' "Nginx must support SPA deep-route fallback."
Assert-Contains $nginxText 'location /api/' "Nginx must define the same-origin API proxy."
Assert-Contains $nginxText 'proxy_pass \$\{INFORMATION_HUB_UPSTREAM\};' "Nginx API upstream must be configurable."
Assert-NotContains $nginxText 'Access-Control-Allow-Origin' "Nginx must not add a wildcard CORS policy."

Write-Host "Static web deployment checks passed."

if ($StaticOnly) {
    exit 0
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is required for rendered Compose validation. Use -StaticOnly only for an explicit local fallback."
}

& docker compose `
    --env-file $envExamplePath `
    --file $composePath `
    --profile web `
    config `
    --quiet

if ($LASTEXITCODE -ne 0) {
    throw "docker compose config validation failed."
}

$renderedJson = & docker compose `
    --env-file $envExamplePath `
    --file $composePath `
    --profile web `
    config `
    --format json

if ($LASTEXITCODE -ne 0) {
    throw "docker compose JSON rendering failed."
}

$config = $renderedJson | ConvertFrom-Json
$web = $config.services.web
if ($null -eq $web) {
    throw "Rendered Compose configuration does not contain the web service."
}

$profileValues = @($web.profiles)
if ($profileValues -notcontains "web") {
    throw "Rendered web service is missing the web profile."
}

$publishedPort = @($web.ports) | Where-Object {
    $_.host_ip -eq "127.0.0.1" -and $_.published -eq "18080" -and $_.target -eq 8080
}
if ($null -eq $publishedPort) {
    throw "Rendered web service must publish 127.0.0.1:18080 to container port 8080."
}

if ($null -eq $web.healthcheck) {
    throw "Rendered web service must define a health check."
}

Write-Host "Rendered Compose web checks passed."
