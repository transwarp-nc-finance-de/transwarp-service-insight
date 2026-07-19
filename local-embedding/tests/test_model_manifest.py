import hashlib
import json
from pathlib import Path

import pytest

from embedding_service.manifest import verify_model


def test_rejects_hash_mismatch_before_model_load(tmp_path: Path) -> None:
    artifact = tmp_path / "model.safetensors"
    artifact.write_bytes(b"tampered")
    allowlist = tmp_path.parent / "allowlist.json"
    allowlist.write_text(
        json.dumps({"files": [{"relativePath": artifact.name, "declaredBytes": 8,
                    "sha256": hashlib.sha256(b"approved").hexdigest()}]}), encoding="utf-8"
    )
    with pytest.raises(ValueError, match="artifact mismatch"):
        verify_model(tmp_path, allowlist)
