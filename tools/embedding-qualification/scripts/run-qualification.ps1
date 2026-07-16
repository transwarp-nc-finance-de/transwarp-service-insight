param(
  [Parameter(Mandatory = $true)]
  [string]$ArtifactRoot,
  [string]$Image = "transwarp-embedding-qualification:issue39"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$artifactRoot = [System.IO.Path]::GetFullPath($ArtifactRoot)
if ($artifactRoot.StartsWith($repoRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Artifact Root must be outside the Git worktree: $artifactRoot"
}

$modelDir = Join-Path $artifactRoot "model"
$evidenceDir = Join-Path $artifactRoot "evidence"
$resultsDir = Join-Path $artifactRoot "results"
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null

$datasetDir = Join-Path $repoRoot "backend\src\main\resources\evaluation\mock-eval-v1"
$modelManifestHash = (Get-Content -Raw (Join-Path $evidenceDir "model.manifest.sha256")).Trim()
$dependencyLockHash = (Get-FileHash -Algorithm SHA256 (Join-Path $repoRoot "tools\embedding-qualification\requirements.lock")).Hash.ToLowerInvariant()
$gitCommit = (git -C $repoRoot rev-parse HEAD).Trim()
$imageDigest = (docker image inspect $Image --format "{{.Id}}").Trim()
$datasetChecksum = (Get-Content -Raw (Join-Path $datasetDir "dataset.sha256")).Trim()

$common = @(
  "run", "--rm",
  "--network", "none",
  "--memory", "4g",
  "--read-only",
  "--tmpfs", "/tmp:rw,noexec,nosuid,size=256m",
  "--tmpfs", "/home/qualification/.cache:rw,noexec,nosuid,size=64m",
  "--mount", "type=bind,source=$modelDir,target=/model,readonly",
  "--mount", "type=bind,source=$datasetDir,target=/dataset,readonly",
  "--mount", "type=bind,source=$resultsDir,target=/output",
  $Image
)

& docker @common qualify `
  --model-dir /model `
  --dataset-dir /dataset `
  --output /output/qualification.json `
  --model-id intfloat/multilingual-e5-base `
  --revision d13f1b27baf31030b7fd040960d60d909913633f `
  --model-manifest-hash $modelManifestHash `
  --image-digest $imageDigest `
  --dependency-lock-hash "sha256:$dependencyLockHash" `
  --dataset-checksum $datasetChecksum `
  --git-commit $gitCommit

& docker @common latency `
  --model-dir /model `
  --dataset-dir /dataset `
  --output /output/query-latency.json `
  --model-manifest-hash $modelManifestHash

& docker @common batch `
  --model-dir /model `
  --dataset-dir /dataset `
  --output /output/batch-index.json
