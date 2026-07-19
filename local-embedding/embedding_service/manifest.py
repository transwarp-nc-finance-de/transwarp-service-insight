from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path

EXPECTED_MANIFEST_SHA256 = "8f58395f4bf0f613fd15ebbf3d5467193ad6717715c4167c764e6e91aae1810e"


def verify_model(model_dir: Path, allowlist_path: Path) -> None:
    model_dir = model_dir.resolve(strict=True)
    entries = json.loads(allowlist_path.read_text(encoding="utf-8"))["files"]
    expected = {entry["relativePath"]: entry for entry in entries}
    actual: dict[str, Path] = {}
    for path in model_dir.rglob("*"):
        if path.is_symlink():
            raise ValueError(f"symbolic links are forbidden: {path}")
        if path.is_file():
            relative = path.relative_to(model_dir).as_posix()
            if "/" in relative or relative in actual:
                raise ValueError(f"unsafe model path: {relative}")
            actual[relative] = path
    if set(actual) != set(expected):
        raise ValueError(
            f"model allowlist mismatch: missing={sorted(set(expected)-set(actual))}, "
            f"unexpected={sorted(set(actual)-set(expected))}"
        )
    manifest_lines: list[str] = []
    for relative in sorted(expected, key=lambda value: value.encode("utf-8")):
        entry = expected[relative]
        path = actual[relative]
        size = os.stat(path, follow_symlinks=False).st_size
        digest = _sha256(path)
        if size != int(entry["declaredBytes"]) or digest != entry["sha256"]:
            raise ValueError(f"model artifact mismatch: {relative}")
        manifest_lines.append(f"{digest}  {size}  {relative}\n")
    manifest_hash = hashlib.sha256("".join(manifest_lines).encode("utf-8")).hexdigest()
    if manifest_hash != EXPECTED_MANIFEST_SHA256:
        raise ValueError(f"manifest SHA-256 mismatch: {manifest_hash}")


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()
