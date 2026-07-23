$ErrorActionPreference = "Stop"

$requiredFiles = @(
    "AGENTS.md",
    "README.md",
    "docs/PROJECT_CONTEXT.md",
    "docs/ARCHITECTURE.md",
    "docs/ROADMAP.md",
    "docs/CURRENT_STATUS.md",
    "docs/DATABASE_DESIGN.md",
    "docs/contracts/information-envelope-v1.md",
    "docs/tasks/TASK-001.md",
    "docs/decisions/ADR-001-use-monorepo.md",
    "collectors/boss-zhipin-scraper/AGENTS.md"
)

$missing = @()
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        $missing += $file
    }
}

if ($missing.Count -gt 0) {
    Write-Error ("Missing required files:`n" + ($missing -join "`n"))
}

Write-Host "Required project files are present."

Write-Host "`n=== Sensitive/unwanted tracked paths check ==="
$tracked = git ls-files
$patterns = @(
    "\.env$",
    "cookies\.json$",
    "storage_state\.json$",
    "chrome-profile",
    "browser-profile",
    "result/job-result",
    "data/outbox"
)

$bad = @()
foreach ($file in $tracked) {
    foreach ($pattern in $patterns) {
        if ($file -match $pattern) {
            $bad += $file
            break
        }
    }
}

if ($bad.Count -gt 0) {
    Write-Error ("Potentially sensitive/runtime files are tracked:`n" + ($bad -join "`n"))
}

Write-Host "No known sensitive/runtime paths are tracked."
