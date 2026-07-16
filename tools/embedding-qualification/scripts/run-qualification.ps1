param(
  [Parameter(Mandatory = $true)]
  [string]$ArtifactRoot,
  [string]$Image = "transwarp-embedding-qualification:issue39"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$artifactRoot = [System.IO.Path]::GetFullPath($ArtifactRoot)
$repoPrefix = $repoRoot.TrimEnd("\") + "\"
if (
  $artifactRoot.Equals($repoRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
  $artifactRoot.StartsWith($repoPrefix, [System.StringComparison]::OrdinalIgnoreCase)
) {
  throw "Artifact Root must be outside the Git worktree: $artifactRoot"
}

$modelDir = Join-Path $artifactRoot "model"
$evidenceDir = Join-Path $artifactRoot "evidence"
$resultsDir = Join-Path $artifactRoot "results"
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null

$datasetDir = Join-Path $repoRoot "backend\src\main\resources\evaluation\mock-eval-v1"
$modelManifestHash = (Get-Content -Raw (Join-Path $evidenceDir "model.manifest.sha256")).Trim()
$dependencyLockHash = (Get-FileHash -Algorithm SHA256 (Join-Path $repoRoot "tools\embedding-qualification\requirements.lock")).Hash.ToLowerInvariant()
$sbomHash = (Get-FileHash -Algorithm SHA256 (Join-Path $repoRoot "tools\embedding-qualification\sbom.cdx.json")).Hash.ToLowerInvariant()
$gitCommit = (git -C $repoRoot rev-parse HEAD).Trim()
$imageDigest = (docker image inspect $Image --format "{{.Id}}").Trim()
$datasetChecksum = (Get-Content -Raw (Join-Path $datasetDir "dataset.sha256")).Trim()
$dockerVersion = (docker version --format "{{.Server.Version}}").Trim()
$dockerCpuLimit = [double](docker info --format "{{.NCPU}}").Trim()
$cpu = Get-CimInstance Win32_Processor | Select-Object -First 1
$computer = Get-CimInstance Win32_ComputerSystem
$os = Get-CimInstance Win32_OperatingSystem

$runningContainers = @(docker ps --format "{{.ID}} {{.Image}} {{.Names}}")
if ($runningContainers.Count -gt 0) {
  throw "Stop all running containers before exclusive performance measurement: $($runningContainers -join '; ')"
}

$preflight = [ordered]@{
  recordedAt = [DateTimeOffset]::UtcNow.ToString("O")
  runningContainers = $runningContainers
  topProcesses = @(Get-Process | Sort-Object WorkingSet64 -Descending | Select-Object -First 20 ProcessName, Id, CPU, WorkingSet64)
  availableMemoryBytes = [int64]$os.FreePhysicalMemory * 1024
  cpuLoadPercent = $cpu.LoadPercentage
  dockerCpuLimit = $dockerCpuLimit
  dockerMemoryLimitBytes = 4294967296
  mockData = $true
}
$preflight | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $resultsDir "preflight.json")

$provenance = [ordered]@{
  modelId = "intfloat/multilingual-e5-base"
  revision = "d13f1b27baf31030b7fd040960d60d909913633f"
  modelManifestHash = $modelManifestHash
  imageDigest = $imageDigest
  dependencyLockHash = "sha256:$dependencyLockHash"
  sbomHash = "sha256:$sbomHash"
  datasetVersion = "mock-eval-v1"
  datasetChecksum = $datasetChecksum
  gitCommit = $gitCommit
  cpuModel = $cpu.Name
  physicalCores = [int]$cpu.NumberOfCores
  logicalProcessors = [int]$cpu.NumberOfLogicalProcessors
  hostMemoryBytes = [int64]$computer.TotalPhysicalMemory
  hostOs = "$($os.Caption) $($os.Version) $($os.OSArchitecture)"
  dockerVersion = $dockerVersion
  cpuLimit = $dockerCpuLimit
  memoryLimitBytes = 4294967296
  threadEnvironment = [ordered]@{
    OMP_NUM_THREADS = "1"
    MKL_NUM_THREADS = "1"
    OPENBLAS_NUM_THREADS = "1"
  }
}
$provenance | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $resultsDir "provenance.json")

$common = @(
  "run", "--rm",
  "--network", "none",
  "--memory", "4g",
  "--read-only",
  "--tmpfs", "/tmp:rw,noexec,nosuid,size=256m",
  "--tmpfs", "/home/qualification/.cache:rw,noexec,nosuid,size=64m",
  "--mount", "type=bind,source=$modelDir,target=/model,readonly",
  "--mount", "type=bind,source=$datasetDir,target=/dataset,readonly",
  "--mount", "type=bind,source=$evidenceDir,target=/evidence,readonly",
  "--mount", "type=bind,source=$resultsDir,target=/output",
  $Image
)

& docker @common verify `
  --allowlist /opt/qualification/allowlist.json `
  --model-dir /model `
  --manifest /evidence/model.manifest

foreach ($run in 1..5) {
  & docker @common smoke `
    --model-dir /model `
    --output "/output/cold-start-$run.json" `
    --provenance /output/provenance.json
}

& docker @common qualify `
  --model-dir /model `
  --dataset-dir /dataset `
  --output /output/qualification.json `
  --provenance /output/provenance.json

& docker @common latency `
  --model-dir /model `
  --dataset-dir /dataset `
  --output /output/query-latency.json `
  --provenance /output/provenance.json

& docker @common batch `
  --model-dir /model `
  --dataset-dir /dataset `
  --output /output/batch-index.json `
  --provenance /output/provenance.json

$resultFiles = Get-ChildItem -LiteralPath $resultsDir -File |
  Where-Object Name -ne "results.sha256" |
  Sort-Object { $_.Name }
$checksumLines = foreach ($file in $resultFiles) {
  $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $file.FullName).Hash.ToLowerInvariant()
  "$hash  $($file.Length)  $($file.Name)"
}
$checksumLines | Set-Content -Encoding ascii (Join-Path $resultsDir "results.sha256")
