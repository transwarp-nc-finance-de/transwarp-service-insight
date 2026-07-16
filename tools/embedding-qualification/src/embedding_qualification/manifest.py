from __future__ import annotations

import hashlib
import json
import os
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Iterable


@dataclass(frozen=True)
class AllowlistEntry:
    relative_path: str
    source_url: str
    declared_bytes: int

    @classmethod
    def from_dict(cls, value: dict[str, object]) -> "AllowlistEntry":
        path = str(value["relativePath"])
        parsed = PurePosixPath(path)
        if (
            parsed.is_absolute()
            or path != parsed.as_posix()
            or ".." in parsed.parts
            or "." in parsed.parts
            or len(parsed.parts) != 1
        ):
            raise ValueError(f"not a safe relative path: {path}")
        declared_bytes = int(value["declaredBytes"])
        if declared_bytes < 0:
            raise ValueError(f"negative declared size: {path}")
        return cls(path, str(value["sourceUrl"]), declared_bytes)


def load_allowlist(path: Path) -> tuple[AllowlistEntry, ...]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    entries = tuple(AllowlistEntry.from_dict(item) for item in payload["files"])
    paths = [entry.relative_path for entry in entries]
    if len(paths) != len(set(paths)):
        raise ValueError("duplicate allowlist path")
    return entries


def _regular_files(root: Path) -> tuple[Path, ...]:
    files: list[Path] = []
    for path in root.rglob("*"):
        if path.is_symlink():
            raise ValueError(f"symbolic links are forbidden: {path}")
        if path.is_file():
            files.append(path)
    return tuple(files)


def validate_artifact_directory(
    root: Path, entries: Iterable[AllowlistEntry]
) -> None:
    root = root.resolve(strict=True)
    expected = {entry.relative_path: entry for entry in entries}
    actual: dict[str, Path] = {}
    for path in _regular_files(root):
        relative = path.relative_to(root).as_posix()
        if relative in actual:
            raise ValueError(f"duplicate path: {relative}")
        actual[relative] = path
    unexpected = sorted(set(actual) - set(expected))
    missing = sorted(set(expected) - set(actual))
    if unexpected:
        raise ValueError(f"unlisted files: {unexpected}")
    if missing:
        raise ValueError(f"missing allowlisted files: {missing}")
    for relative, entry in expected.items():
        size = os.stat(actual[relative], follow_symlinks=False).st_size
        if size != entry.declared_bytes:
            raise ValueError(
                f"declared byte mismatch for {relative}: "
                f"expected {entry.declared_bytes}, got {size}"
            )


def sha256_file(path: Path, chunk_size: int = 1024 * 1024) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while chunk := stream.read(chunk_size):
            digest.update(chunk)
    return digest.hexdigest()


def build_manifest(root: Path, entries: Iterable[AllowlistEntry]) -> str:
    entries = tuple(entries)
    validate_artifact_directory(root, entries)
    ordered = sorted(entries, key=lambda item: item.relative_path.encode("utf-8"))
    return "".join(
        f"{sha256_file(root / entry.relative_path)}  "
        f"{entry.declared_bytes}  {entry.relative_path}\n"
        for entry in ordered
    )


def manifest_sha256(manifest: str) -> str:
    return hashlib.sha256(manifest.encode("utf-8")).hexdigest()
