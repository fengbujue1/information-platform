$ErrorActionPreference = "Stop"

Write-Host "=== Git status ==="
git status

Write-Host "`n=== Recent commits ==="
git log --oneline -5

if (Test-Path "backend/information-hub/mvnw.cmd") {
    Write-Host "`n=== Backend tests ==="
    Push-Location "backend/information-hub"
    .\mvnw.cmd test
    Pop-Location
} else {
    Write-Host "`nBackend project not created yet; skipping Maven tests."
}

Write-Host "`n确认 docs/CURRENT_STATUS.md 和当前 TASK 已更新。"
Write-Host "随后人工检查 git diff，再 commit 和 push。"
