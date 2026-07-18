from __future__ import annotations

import datetime as dt
import json
import os
import shutil
import subprocess
from pathlib import Path

from .manifest import AllowlistEntry, sha256_file


def qualification_curl() -> str:
    configured = os.environ.get("QUALIFICATION_CURL")
    if not configured:
        raise ValueError(
            "QUALIFICATION_CURL must name a reviewed absolute executable path"
        )
    executable = Path(configured)
    if not executable.is_absolute():
        raise ValueError("QUALIFICATION_CURL must be an absolute path")
    if not executable.is_file():
        raise ValueError("QUALIFICATION_CURL executable does not exist")
    return str(executable)


def curl_version_command(executable: str) -> list[str]:
    return [executable, "--disable", "--version"]


def curl_version(executable: str) -> str:
    output = subprocess.run(
        curl_version_command(executable),
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
    ).stdout.splitlines()[0]
    return output.strip()


def curl_fetch_command(
    executable: str,
    target: str,
    source_url: str,
) -> list[str]:
    return [
        executable,
        "--disable",
        "--fail",
        "--location",
        "--silent",
        "--show-error",
        "--proto",
        "=https",
        "--tlsv1.2",
        "--output",
        target,
        source_url,
    ]


def fetch_allowlisted_files(
    model_dir: Path,
    entries: tuple[AllowlistEntry, ...],
    model_id: str,
    revision: str,
) -> list[dict[str, object]]:
    model_dir.mkdir(parents=True, exist_ok=True)
    if any(model_dir.iterdir()):
        raise ValueError("model directory must be empty before controlled fetch")
    executable = qualification_curl()
    retrieval_version = curl_version(executable)
    records: list[dict[str, object]] = []
    try:
        for entry in entries:
            target = model_dir / entry.relative_path
            temporary = target.with_suffix(target.suffix + ".part")
            subprocess.run(
                curl_fetch_command(
                    executable,
                    str(temporary),
                    entry.source_url,
                ),
                check=True,
            )
            actual_bytes = os.stat(temporary).st_size
            if actual_bytes != entry.declared_bytes:
                raise ValueError(
                    f"remote byte mismatch for {entry.relative_path}: "
                    f"expected {entry.declared_bytes}, got {actual_bytes}"
                )
            digest = sha256_file(temporary)
            if digest != entry.sha256:
                raise ValueError(
                    f"remote SHA-256 mismatch for {entry.relative_path}: "
                    f"expected {entry.sha256}, got {digest}"
                )
            os.replace(temporary, target)
            records.append(
                {
                    "relativePath": entry.relative_path,
                    "sourceUrl": entry.source_url,
                    "modelId": model_id,
                    "revision": revision,
                    "byteSize": actual_bytes,
                    "sha256": digest,
                    "downloadedAt": dt.datetime.now(dt.UTC)
                    .replace(microsecond=0)
                    .isoformat(),
                    "retrievalToolVersion": retrieval_version,
                }
            )
    except Exception:
        shutil.rmtree(model_dir, ignore_errors=True)
        raise
    return records
