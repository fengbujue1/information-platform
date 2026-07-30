$ErrorActionPreference = "Stop"

$requiredFiles = @(
    "AGENTS.md",
    "README.md",
    "docs/PROJECT_CONTEXT.md",
    "docs/ARCHITECTURE.md",
    "docs/ROADMAP.md",
    "docs/CURRENT_STATUS.md",
    "docs/DATABASE_DESIGN.md",
    "docs/PHASE2_SCOPE.md",
    "docs/contracts/information-envelope-v1.md",
    "docs/contracts/job-query-api-v1.md",
    "docs/contracts/web-ui-behavior-v1.md",
    "docs/tasks/TASK-001.md",
    "docs/tasks/TASK-018.md",
    "docs/decisions/ADR-001-use-monorepo.md",
    "collectors/boss-zhipin-scraper/AGENTS.md",
    "backend/information-hub/AGENTS.md",
    "frontend/information-hub-web/AGENTS.md"
)

$missing = @()
foreach ($file in $requiredFiles) {
    if (-not (Test-Path -LiteralPath $file)) {
        $missing += $file
    }
}

if ($missing.Count -gt 0) {
    Write-Error ("Missing required files:`n" + ($missing -join "`n"))
}

Write-Host "Required project files are present."

Write-Host "`n=== Sensitive/unwanted tracked paths check ==="
$tracked = @(git ls-files)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to list tracked files."
}

$bad = @()
foreach ($file in $tracked) {
    $normalized = $file.Replace("\", "/").ToLowerInvariant()
    $leaf = [System.IO.Path]::GetFileName($normalized)

    $isSensitiveEnvironmentFile =
        $leaf -eq ".env" -or
        ($leaf.StartsWith(".env.") -and -not $leaf.EndsWith(".example"))
    $hasSensitiveExtension = $normalized -match '\.(key|pem|p12|pfx)$'
    $isKnownPrivateConfig =
        $normalized -eq "backend/information-hub/config/application.yml" -or
        $normalized -eq "collectors/boss-zhipin-scraper/config/collector.ini"
    $isRuntimePath =
        $normalized -match '(^|/)(secrets?|chrome-profile|browser-profile|user-data-dir)(/|$)' -or
        $normalized -match '^collectors/boss-zhipin-scraper/(result/job-result|result/outbox|data/outbox|data/raw|output)(/|$)' -or
        $normalized -match '^deploy/(data|mysql/data|mysql/backups)(/|$)' -or
        $normalized -match '(^|/)(cookies\.json|storage_state\.json)$'

    if ($isSensitiveEnvironmentFile -or $hasSensitiveExtension -or $isKnownPrivateConfig -or $isRuntimePath) {
        $bad += $file
    }
}

if ($bad.Count -gt 0) {
    Write-Error ("Potentially sensitive/runtime files are tracked:`n" + (($bad | Sort-Object -Unique) -join "`n"))
}

$privateKeyMarkers = @(git grep -n -I -E -- "-----BEGIN (OPENSSH|RSA|EC|DSA|PRIVATE) PRIVATE KEY-----")
if ($LASTEXITCODE -eq 0 -and $privateKeyMarkers.Count -gt 0) {
    Write-Error ("Private key material appears in tracked files:`n" + ($privateKeyMarkers -join "`n"))
}
if ($LASTEXITCODE -gt 1) {
    throw "Unable to scan tracked files for private key material."
}

Write-Host "No known sensitive/runtime paths or private key markers are tracked."
