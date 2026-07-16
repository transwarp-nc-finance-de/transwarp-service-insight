import unittest

from embedding_qualification.provenance import QualificationProvenance


class ProvenanceTest(unittest.TestCase):
    def test_rejects_non_four_gib_memory_limit(self) -> None:
        value = {
            "modelId": "intfloat/multilingual-e5-base",
            "revision": "d" * 40,
            "modelManifestHash": "a" * 64,
            "imageDigest": "sha256:" + "b" * 64,
            "dependencyLockHash": "sha256:" + "c" * 64,
            "sbomHash": "sha256:" + "d" * 64,
            "datasetVersion": "mock-eval-v1",
            "datasetChecksum": "sha256:" + "e" * 64,
            "gitCommit": "f" * 40,
            "cpuModel": "mock cpu",
            "physicalCores": 6,
            "logicalProcessors": 12,
            "hostMemoryBytes": 32_000_000_000,
            "hostOs": "mock os",
            "dockerVersion": "mock docker",
            "cpuLimit": 12,
            "memoryLimitBytes": 4_000_000_000,
            "threadEnvironment": {
                "OMP_NUM_THREADS": "1",
                "MKL_NUM_THREADS": "1",
                "OPENBLAS_NUM_THREADS": "1",
            },
        }

        with self.assertRaisesRegex(ValueError, "exactly 4 GiB"):
            QualificationProvenance.from_dict(value)


if __name__ == "__main__":
    unittest.main()
