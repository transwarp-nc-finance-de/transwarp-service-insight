from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable


@dataclass(frozen=True)
class CaseResult:
    case_id: str
    expected_ids: tuple[str, ...]
    forbidden_ids: tuple[str, ...]
    returned_ids: tuple[str, ...]
    citation_valid: bool
    degradation_passed: bool


@dataclass(frozen=True)
class QualificationMetrics:
    execution_status: str
    gate_passed: bool
    permission_leakage_rate: float
    citation_error_rate: float
    degradation_pass_rate: float
    recall_at_5: float


def calculate_metrics(results: Iterable[CaseResult]) -> QualificationMetrics:
    results = tuple(results)
    if not results:
        raise ValueError("at least one case result is required")
    leaking = sum(
        bool(set(result.returned_ids).intersection(result.forbidden_ids))
        for result in results
    )
    citation_errors = sum(not result.citation_valid for result in results)
    degradation_passes = sum(result.degradation_passed for result in results)
    recall_cases = [result for result in results if result.expected_ids]
    recall = (
        sum(
            len(set(result.returned_ids[:5]).intersection(result.expected_ids))
            / len(result.expected_ids)
            for result in recall_cases
        )
        / len(recall_cases)
        if recall_cases
        else 1.0
    )
    leakage_rate = leaking / len(results)
    citation_error_rate = citation_errors / len(results)
    degradation_pass_rate = degradation_passes / len(results)
    gate_passed = (
        leakage_rate == 0.0
        and citation_error_rate == 0.0
        and degradation_pass_rate == 1.0
        and recall >= 0.8
    )
    return QualificationMetrics(
        execution_status="SUCCEEDED",
        gate_passed=gate_passed,
        permission_leakage_rate=leakage_rate,
        citation_error_rate=citation_error_rate,
        degradation_pass_rate=degradation_pass_rate,
        recall_at_5=recall,
    )
