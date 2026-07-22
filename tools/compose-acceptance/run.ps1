param(
    [Parameter(Mandatory = $true)]
    [string]$ModelPath,
    [switch]$KeepEnvironment
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$frontendRoot = Join-Path $repoRoot "frontend"
$manifestPath = Join-Path $repoRoot "tools\embedding-qualification\evidence\model.manifest"
$composeArgs = @("compose", "-p", "service-insight-issue29", "-f", (Join-Path $repoRoot "compose.yaml"))
$env:LOCAL_EMBEDDING_MODEL_PATH = (Resolve-Path $ModelPath).Path

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Command)
    & docker @composeArgs @Command
    if ($LASTEXITCODE -ne 0) { throw "docker compose failed: $($Command -join ' ')" }
}

function Invoke-Frontend {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Command)
    Push-Location $frontendRoot
    try {
        & npm @Command
        if ($LASTEXITCODE -ne 0) { throw "npm failed: $($Command -join ' ')" }
    }
    finally { Pop-Location }
}

function Assert-ApprovedModel {
    $expectedFiles = 0
    foreach ($line in Get-Content $manifestPath) {
        if ($line -notmatch '^([0-9a-f]{64})\s+(\d+)\s+(.+)$') { continue }
        $expectedFiles++
        $expectedHash = $Matches[1]
        $expectedBytes = [int64]$Matches[2]
        $relativePath = $Matches[3]
        $artifact = Join-Path $env:LOCAL_EMBEDDING_MODEL_PATH $relativePath
        if (-not (Test-Path -LiteralPath $artifact -PathType Leaf)) {
            throw "Approved model artifact is missing: $relativePath"
        }
        $item = Get-Item -LiteralPath $artifact
        $actualHash = (Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($item.Length -ne $expectedBytes -or $actualHash -ne $expectedHash) {
            throw "Approved model artifact mismatch: $relativePath"
        }
    }
    if ($expectedFiles -ne 5) { throw "Expected exactly five approved model artifacts." }
}

function Assert-Healthy {
    $services = Invoke-Compose ps --format json | ForEach-Object { $_ | ConvertFrom-Json }
    foreach ($name in @("postgres", "local-embedding", "backend", "frontend")) {
        $service = $services | Where-Object Service -eq $name
        if (-not $service -or $service.State -ne "running") { throw "Service is not running: $name" }
    }
    foreach ($name in @("postgres", "local-embedding", "backend", "frontend")) {
        $service = $services | Where-Object Service -eq $name
        if ($service.Health -ne "healthy") { throw "Service is not healthy: $name" }
    }
    $health = Invoke-RestMethod -Uri "http://127.0.0.1:5173/api/v1/health" -TimeoutSec 10
    if ($health.status -ne "UP") { throw "Frontend-to-backend health route is not UP." }
}

function Get-PersistenceSnapshot {
    $sql = @"
SELECT json_build_object(
  'documents',(SELECT count(*) FROM knowledge_document),
  'versions',(SELECT count(*) FROM knowledge_version_v2),
  'chunks',(SELECT count(*) FROM knowledge_chunk_index),
  'sessions',(SELECT count(*) FROM precheck_session_v2),
  'runs',(SELECT count(*) FROM precheck_run_v2),
  'feedback',(SELECT count(*) FROM precheck_feedback_v2),
  'continuations',(SELECT count(*) FROM submission_continuation_v2),
  'audits',(SELECT count(*) FROM audit_event_v2),
  'evaluations',(SELECT count(*) FROM evaluation_run));
"@
    $databaseSnapshot = Invoke-Compose -Command @(
        "exec", "-T", "postgres", "psql", "-U", "service_insight", "-d", "service_insight", "-tAc", $sql
    )
    if ($LASTEXITCODE -ne 0) { throw "Could not read persistence snapshot." }
    $fileSnapshot = Invoke-Compose -Command @(
        "exec", "-T", "backend", "sh", "-c",
        "find /var/lib/service-insight/knowledge -type f -print0 | sort -z | xargs -0 -r sha256sum"
    )
    if ($LASTEXITCODE -ne 0) { throw "Could not hash persisted knowledge files." }
    return (($databaseSnapshot | Out-String).Trim() + "`n" + ($fileSnapshot | Out-String).Trim())
}

$completed = $false
try {
    Assert-ApprovedModel
    Invoke-Compose down --volumes --remove-orphans
    Invoke-Compose up -d --build --wait
    Assert-Healthy

    Invoke-Frontend run test:e2e:full
    Invoke-Frontend run test:e2e:v1

    Invoke-Compose stop local-embedding
    Invoke-Frontend run test:e2e:runtime-fault

    Invoke-Compose start local-embedding
    Invoke-Compose up -d --wait local-embedding
    $beforeRestart = Get-PersistenceSnapshot
    Invoke-Compose restart postgres backend
    Invoke-Compose up -d --wait postgres backend
    Assert-Healthy
    $afterRestart = Get-PersistenceSnapshot
    if ($afterRestart -ne $beforeRestart) {
        throw "Persistence snapshot changed across PostgreSQL/backend restart."
    }

    Invoke-Frontend run test:e2e:recovery

    # FTS-only is a distinct deployment profile. Recreate it with empty,
    # isolated volumes so the browser cannot restore a HYBRID session from the
    # preceding full-mode recovery phase.
    Invoke-Compose down --volumes --remove-orphans
    $composeArgs += @("-f", (Join-Path $repoRoot "compose.fts-only.yaml"))
    Invoke-Compose up -d --build --wait
    Invoke-Frontend run test:e2e:ci
    $completed = $true
    Write-Host "Issue #29 Compose acceptance passed with simulated data."
}
finally {
    if (-not $KeepEnvironment) {
        Invoke-Compose down --volumes --remove-orphans
    }
    elseif (-not $completed) {
        Write-Warning "Acceptance failed; isolated environment was kept for diagnosis."
    }
}
