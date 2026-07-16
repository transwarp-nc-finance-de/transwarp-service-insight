import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from embedding_qualification.manifest import (
    AllowlistEntry,
    build_manifest,
    validate_artifact_directory,
)


class ManifestTest(unittest.TestCase):
    def test_build_manifest_uses_utf8_path_order_and_actual_bytes(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "z.json").write_bytes(b"z")
            (root / "a.json").write_bytes(b"alpha")
            entries = (
                AllowlistEntry(
                    "z.json",
                    "https://example.invalid/z",
                    1,
                    hashlib.sha256(b"z").hexdigest(),
                ),
                AllowlistEntry(
                    "a.json",
                    "https://example.invalid/a",
                    5,
                    hashlib.sha256(b"alpha").hexdigest(),
                ),
            )

            manifest = build_manifest(root, entries)

            self.assertEqual(
                manifest,
                (
                    f"{hashlib.sha256(b'alpha').hexdigest()}  5  a.json\n"
                    f"{hashlib.sha256(b'z').hexdigest()}  1  z.json\n"
                ),
            )

    def test_validation_rejects_unlisted_files(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "config.json").write_text("{}", encoding="utf-8")
            (root / "unexpected.bin").write_bytes(b"x")
            entries = (
                AllowlistEntry(
                    "config.json",
                    "https://example.invalid/config",
                    2,
                    hashlib.sha256(b"{}").hexdigest(),
                ),
            )

            with self.assertRaisesRegex(ValueError, "unlisted"):
                validate_artifact_directory(root, entries)

    def test_allowlist_paths_must_be_safe_relative_paths(self) -> None:
        payload = {
            "files": [
                {
                    "relativePath": "../model.safetensors",
                    "sourceUrl": "https://example.invalid/model",
                    "declaredBytes": 1,
                    "sha256": hashlib.sha256(b"x").hexdigest(),
                }
            ]
        }

        with self.assertRaisesRegex(ValueError, "safe relative path"):
            AllowlistEntry.from_dict(payload["files"][0])


if __name__ == "__main__":
    unittest.main()
