#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
compose_args=(
  -f "$repo_root/compose.yaml"
  -f "$repo_root/compose.fts-only.yaml"
)

cd "$repo_root"
docker compose "${compose_args[@]}" config --quiet
docker compose "${compose_args[@]}" up -d --build --wait
curl --fail --silent http://127.0.0.1:5173/api/v1/health |
  grep --quiet '"status"[[:space:]]*:[[:space:]]*"UP"'

printf '%s\n' \
  "Service Insight is available at http://127.0.0.1:5173" \
  "Mode: FTS_ONLY (no Embedding model is included or downloaded)"
