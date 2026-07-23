param()

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$composeFiles = @(
    "-f", (Join-Path $repoRoot "compose.yaml"),
    "-f", (Join-Path $repoRoot "compose.fts-only.yaml")
)

Push-Location $repoRoot
try {
    & docker compose @composeFiles config --quiet
    if ($LASTEXITCODE -ne 0) { throw "Compose configuration validation failed." }

    & docker compose @composeFiles up -d --build --wait
    if ($LASTEXITCODE -ne 0) { throw "FTS-only startup failed." }

    $health = Invoke-RestMethod -Uri "http://127.0.0.1:5173/api/v1/health" -TimeoutSec 10
    if ($health.status -ne "UP") { throw "Frontend-to-backend health route is not UP." }

    Write-Host "Service Insight is available at http://127.0.0.1:5173"
    Write-Host "Mode: FTS_ONLY (no Embedding model is included or downloaded)"
}
finally {
    Pop-Location
}
