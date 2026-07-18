import unittest

from embedding_qualification.metrics import CaseResult, calculate_metrics


class MetricsTest(unittest.TestCase):
    def test_metrics_distinguish_succeeded_execution_from_failed_gate(self) -> None:
        results = (
            CaseResult(
                case_id="case-1",
                expected_ids=("e-1",),
                forbidden_ids=(),
                returned_ids=("e-other",),
                citation_valid=True,
                degradation_passed=True,
            ),
        )

        metrics = calculate_metrics(results)

        self.assertEqual(metrics.execution_status, "SUCCEEDED")
        self.assertFalse(metrics.gate_passed)
        self.assertEqual(metrics.recall_at_5, 0.0)

    def test_gate_thresholds_match_issue_39(self) -> None:
        results = (
            CaseResult(
                case_id="case-1",
                expected_ids=("e-1",),
                forbidden_ids=("forbidden",),
                returned_ids=("e-1",),
                citation_valid=True,
                degradation_passed=True,
            ),
        )

        metrics = calculate_metrics(results)

        self.assertTrue(metrics.gate_passed)
        self.assertEqual(metrics.permission_leakage_rate, 0.0)
        self.assertEqual(metrics.citation_error_rate, 0.0)
        self.assertEqual(metrics.degradation_pass_rate, 1.0)
        self.assertEqual(metrics.recall_at_5, 1.0)


if __name__ == "__main__":
    unittest.main()
