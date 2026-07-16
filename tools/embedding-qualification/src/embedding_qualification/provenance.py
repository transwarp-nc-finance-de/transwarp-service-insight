from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any

from . import HARNESS_VERSION

_SHA256 = re.compile(r"^(?:sha256:)?[0-9a-f]{64}$")
_GIT_COMMIT = re.compile(r"^[0-9a-f]{40}$")


@dataclass(frozen=True)
class QualificationProvenance:
    model_id: str
    revision: str
    model_manifest_hash: str
    image_digest: str
    dependency_lock_hash: str
    sbom_hash: str
    dataset_version: str
    dataset_checksum: str
    git_commit: str
    cpu_model: str
    physical_cores: int
    logical_processors: int
    host_memory_bytes: int
    host_os: str
    docker_version: str
    cpu_limit: float
    memory_limit_bytes: int
    thread_environment: dict[str, str]

    @classmethod
    def from_dict(cls, value: dict[str, Any]) -> "QualificationProvenance":
        result = cls(
            model_id=str(value["modelId"]),
            revision=str(value["revision"]),
            model_manifest_hash=str(value["modelManifestHash"]).lower(),
            image_digest=str(value["imageDigest"]).lower(),
            dependency_lock_hash=str(value["dependencyLockHash"]).lower(),
            sbom_hash=str(value["sbomHash"]).lower(),
            dataset_version=str(value["datasetVersion"]),
            dataset_checksum=str(value["datasetChecksum"]).lower(),
            git_commit=str(value["gitCommit"]).lower(),
            cpu_model=str(value["cpuModel"]),
            physical_cores=int(value["physicalCores"]),
            logical_processors=int(value["logicalProcessors"]),
            host_memory_bytes=int(value["hostMemoryBytes"]),
            host_os=str(value["hostOs"]),
            docker_version=str(value["dockerVersion"]),
            cpu_limit=float(value["cpuLimit"]),
            memory_limit_bytes=int(value["memoryLimitBytes"]),
            thread_environment={
                str(key): str(item)
                for key, item in value["threadEnvironment"].items()
            },
        )
        result._validate()
        return result

    def _validate(self) -> None:
        for name, value in (
            ("modelManifestHash", self.model_manifest_hash),
            ("imageDigest", self.image_digest),
            ("dependencyLockHash", self.dependency_lock_hash),
            ("sbomHash", self.sbom_hash),
            ("datasetChecksum", self.dataset_checksum),
        ):
            if not _SHA256.fullmatch(value):
                raise ValueError(f"invalid {name}: {value}")
        if not _GIT_COMMIT.fullmatch(self.revision):
            raise ValueError(f"model revision must be a 40-character hash: {self.revision}")
        if not _GIT_COMMIT.fullmatch(self.git_commit):
            raise ValueError(f"invalid Git commit: {self.git_commit}")
        if self.memory_limit_bytes != 4_294_967_296:
            raise ValueError("qualification memory limit must be exactly 4 GiB")
        if min(
            self.physical_cores,
            self.logical_processors,
            self.host_memory_bytes,
        ) <= 0:
            raise ValueError("host resource values must be positive")
        expected_threads = {
            "OMP_NUM_THREADS": "1",
            "MKL_NUM_THREADS": "1",
            "OPENBLAS_NUM_THREADS": "1",
        }
        if self.thread_environment != expected_threads:
            raise ValueError("thread environment does not match qualification profile")

    def to_dict(self) -> dict[str, Any]:
        return {
            "modelId": self.model_id,
            "revision": self.revision,
            "modelManifestHash": self.model_manifest_hash,
            "imageDigest": self.image_digest,
            "dependencyLockHash": self.dependency_lock_hash,
            "sbomHash": self.sbom_hash,
            "datasetVersion": self.dataset_version,
            "datasetChecksum": self.dataset_checksum,
            "harnessVersion": HARNESS_VERSION,
            "gitCommit": self.git_commit,
            "cpuModel": self.cpu_model,
            "physicalCores": self.physical_cores,
            "logicalProcessors": self.logical_processors,
            "hostMemoryBytes": self.host_memory_bytes,
            "hostOs": self.host_os,
            "dockerVersion": self.docker_version,
            "cpuLimit": self.cpu_limit,
            "memoryLimitBytes": self.memory_limit_bytes,
            "threadEnvironment": self.thread_environment,
        }


def intelligent_output_context() -> dict[str, Any]:
    return {
        "mockDataStatement": "模拟数据：小样本工程评估，不代表生产效果。",
        "confidence": "HIGH",
        "evidenceSources": [
            "Issue #39",
            "docs/development/embedding-model-qualification.md",
            "backend/src/main/resources/evaluation/mock-eval-v1",
        ],
        "humanInterventionRecommendation": (
            "由人工复核每项门禁与原始 checksum 后决定 Issue #19 的 "
            "PASS/FAIL/BLOCKED；不得自动关闭门禁或阻断人工继续提交。"
        ),
        "missingInformation": [],
    }
