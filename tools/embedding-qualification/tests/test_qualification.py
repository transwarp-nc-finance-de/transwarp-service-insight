import unittest

from embedding_qualification.qualification import (
    EmbeddingUnavailable,
    QualificationRunner,
)


class RecordingEmbedder:
    def __init__(self) -> None:
        self.calls: list[tuple[str, ...]] = []

    def embed(self, texts: list[str], prefix: str) -> list[list[float]]:
        self.calls.append(tuple(texts))
        return [[1.0, 0.0] for _ in texts]


class FailingEmbedder:
    def embed(self, texts: list[str], prefix: str) -> list[list[float]]:
        raise EmbeddingUnavailable("injected failure seam")


class QualificationRunnerTest(unittest.TestCase):
    def test_all_turns_are_executed_in_sequence(self) -> None:
        embedder = RecordingEmbedder()
        dataset = {
            "datasetVersion": "mock-eval-v1",
            "cases": [
                {
                    "caseId": "case-multi",
                    "scenarioTags": ["MULTI_RUN"],
                    "languageTags": ["LANG_MIXED"],
                    "turns": [
                        {
                            "runSequence": 1,
                            "contextSnapshot": {
                                "title": "first",
                                "descriptionPlainText": "alpha",
                                "additionalInformation": [],
                                "productLineCode": "TDH",
                            },
                        },
                        {
                            "runSequence": 2,
                            "contextSnapshot": {
                                "title": "second",
                                "descriptionPlainText": "beta",
                                "additionalInformation": [],
                                "productLineCode": "TDH",
                            },
                        },
                    ],
                    "executionIdentityCode": "identity",
                    "allowedProductLineCodes": ["TDH"],
                    "expectedEvidenceIds": ["e-1"],
                    "forbiddenEvidenceIds": [],
                    "expectedRetrievalMode": "HYBRID",
                    "expectedDegradation": "NONE",
                    "expectedMissingFieldCodes": [],
                }
            ],
        }
        manifest = {
            "executionIdentities": [
                {
                    "identityCode": "identity",
                    "productLineCodes": ["TDH"],
                }
            ],
            "evidenceFixtures": [
                {
                    "evidenceId": "e-1",
                    "productLineCode": "TDH",
                    "versionId": "v-1",
                    "chunkId": "c-1",
                    "document": {"documentId": "d-1", "title": "alpha beta"},
                    "excerpt": "alpha beta",
                    "contentHash": (
                        "sha256:"
                        "1a989ea86150171c687b0727f218eedbb94c4665a7da"
                        "9b0add1bf5de607f2bf1"
                    ),
                }
            ],
        }

        output = QualificationRunner(embedder).run(dataset, manifest)

        self.assertEqual(
            [turn["runSequence"] for turn in output["caseResults"][0]["turns"]],
            [1, 2],
        )
        self.assertEqual(len(embedder.calls), 3)

    def test_embedding_failure_uses_real_exception_seam(self) -> None:
        dataset = {
            "datasetVersion": "mock-eval-v1",
            "cases": [
                {
                    "caseId": "case-fallback",
                    "scenarioTags": ["EMBEDDING_DEGRADATION"],
                    "languageTags": ["LANG_EN"],
                    "turns": [
                        {
                            "runSequence": 1,
                            "contextSnapshot": {
                                "title": "fallback",
                                "descriptionPlainText": "authorized full-text",
                                "additionalInformation": [],
                                "productLineCode": "TDH",
                            },
                        }
                    ],
                    "executionIdentityCode": "identity",
                    "allowedProductLineCodes": ["TDH"],
                    "expectedEvidenceIds": ["e-1"],
                    "forbiddenEvidenceIds": [],
                    "expectedRetrievalMode": "FTS_ONLY",
                    "expectedDegradation": "FTS_ONLY",
                    "expectedMissingFieldCodes": [],
                }
            ],
        }
        manifest = {
            "executionIdentities": [
                {
                    "identityCode": "identity",
                    "productLineCodes": ["TDH"],
                }
            ],
            "evidenceFixtures": [
                {
                    "evidenceId": "e-1",
                    "productLineCode": "TDH",
                    "versionId": "v-1",
                    "chunkId": "c-1",
                    "document": {"documentId": "d-1", "title": "fallback"},
                    "excerpt": "authorized full-text",
                    "contentHash": (
                        "sha256:"
                        "b71dcc5bca1e0320bf6671e64124059349e4deace1e0"
                        "c7e25677d16098915237"
                    ),
                }
            ],
        }

        output = QualificationRunner(FailingEmbedder()).run(dataset, manifest)

        turn = output["caseResults"][0]["turns"][0]
        self.assertEqual(turn["retrievalMode"], "FTS_ONLY")
        self.assertEqual(turn["degradation"], "FTS_ONLY")

    def test_unavailable_scenario_injects_both_retrieval_failures(self) -> None:
        dataset = {
            "datasetVersion": "mock-eval-v1",
            "cases": [
                {
                    "caseId": "case-unavailable",
                    "scenarioTags": ["EMBEDDING_DEGRADATION", "UNAVAILABLE"],
                    "languageTags": ["LANG_MIXED"],
                    "turns": [
                        {
                            "runSequence": 1,
                            "contextSnapshot": {
                                "title": "fallback",
                                "descriptionPlainText": "authorized full-text",
                                "additionalInformation": [],
                                "productLineCode": "TDH",
                            },
                        }
                    ],
                    "executionIdentityCode": "identity",
                    "allowedProductLineCodes": ["TDH"],
                    "expectedEvidenceIds": [],
                    "forbiddenEvidenceIds": [],
                    "expectedRetrievalMode": "NONE",
                    "expectedDegradation": "UNAVAILABLE",
                    "expectedMissingFieldCodes": [],
                }
            ],
        }
        manifest = {
            "executionIdentities": [
                {
                    "identityCode": "identity",
                    "productLineCodes": ["TDH"],
                }
            ],
            "evidenceFixtures": [
                {
                    "evidenceId": "e-1",
                    "productLineCode": "TDH",
                    "versionId": "v-1",
                    "chunkId": "c-1",
                    "document": {"documentId": "d-1", "title": "fallback"},
                    "excerpt": "authorized full-text",
                    "contentHash": (
                        "sha256:"
                        "b71dcc5bca1e0320bf6671e64124059349e4deace1e0"
                        "c7e25677d16098915237"
                    ),
                }
            ],
        }

        output = QualificationRunner(FailingEmbedder()).run(dataset, manifest)

        turn = output["caseResults"][0]["turns"][0]
        self.assertEqual(turn["retrievalMode"], "NONE")
        self.assertEqual(turn["degradation"], "UNAVAILABLE")
        self.assertEqual(turn["topEvidenceIds"], [])


if __name__ == "__main__":
    unittest.main()
