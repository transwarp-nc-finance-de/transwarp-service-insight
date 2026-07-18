import unittest

from embedding_qualification.retrieval import (
    Evidence,
    filter_authorized_evidence,
    reciprocal_rank_fusion,
)


class RetrievalTest(unittest.TestCase):
    def test_authorization_happens_before_candidates_are_returned(self) -> None:
        evidence = (
            Evidence("e-streaming", "STREAMING", "v2", "c2", "stream"),
            Evidence("e-tdh", "TDH", "v1", "c1", "tdh"),
        )

        authorized = filter_authorized_evidence(
            evidence,
            identity_product_lines={"TDH"},
            case_product_lines={"TDH"},
        )

        self.assertEqual([item.evidence_id for item in authorized], ["e-tdh"])

    def test_rrf_uses_k_60_and_stable_tie_breakers(self) -> None:
        evidence = {
            "e-b": Evidence("e-b", "TDH", "v2", "c2", "b"),
            "e-a": Evidence("e-a", "TDH", "v1", "c1", "a"),
        }

        fused = reciprocal_rank_fusion(
            lexical_ids=["e-b", "e-a"],
            vector_ids=["e-a", "e-b"],
            evidence_by_id=evidence,
            k=60,
            limit=5,
        )

        self.assertEqual([item.evidence_id for item in fused], ["e-a", "e-b"])


if __name__ == "__main__":
    unittest.main()
