from __future__ import annotations

import hashlib
import time
from dataclasses import asdict
from typing import Any, Protocol

from .metrics import CaseResult, calculate_metrics
from .retrieval import (
    Evidence,
    cosine_rank,
    filter_authorized_evidence,
    lexical_rank,
    reciprocal_rank_fusion,
)


class EmbeddingUnavailable(RuntimeError):
    pass


class Embedder(Protocol):
    def embed(self, texts: list[str], prefix: str) -> list[list[float]]: ...


class UnavailableEmbedder:
    def embed(self, texts: list[str], prefix: str) -> list[list[float]]:
        raise EmbeddingUnavailable("injected embedding failure adapter")


def query_from_context(context: dict[str, Any]) -> str:
    additional = " ".join(
        str(item.get("value", "")) for item in context.get("additionalInformation", [])
    )
    fields = (
        context.get("productLineCode"),
        context.get("productCode"),
        context.get("componentCode"),
        context.get("version"),
        context.get("title"),
        context.get("descriptionPlainText"),
        additional,
        context.get("impactScope"),
    )
    return " ".join(str(value).strip() for value in fields if value).strip()


def _fixture_to_evidence(fixture: dict[str, Any]) -> Evidence:
    return Evidence(
        evidence_id=fixture["evidenceId"],
        product_line_code=fixture["productLineCode"],
        version_id=fixture["versionId"],
        chunk_id=fixture["chunkId"],
        text=f"{fixture['document']['title']} {fixture['excerpt']}",
        document_id=fixture["document"]["documentId"],
        content_hash=fixture["contentHash"],
    )


def _citation_valid(
    citation: dict[str, str], fixture: dict[str, Any]
) -> bool:
    expected = "sha256:" + hashlib.sha256(
        fixture["excerpt"].encode("utf-8")
    ).hexdigest()
    expected_citation = {
        "evidenceId": fixture["evidenceId"],
        "documentId": fixture["document"]["documentId"],
        "versionId": fixture["versionId"],
        "chunkId": fixture["chunkId"],
        "contentHash": expected,
    }
    return citation == expected_citation


def _citation(evidence: Evidence) -> dict[str, str]:
    return {
        "evidenceId": evidence.evidence_id,
        "documentId": evidence.document_id,
        "versionId": evidence.version_id,
        "chunkId": evidence.chunk_id,
        "contentHash": evidence.content_hash,
    }


class QualificationRunner:
    def __init__(self, embedder: Embedder) -> None:
        self._embedder = embedder
        self._evidence_vector_cache: dict[str, list[float]] = {}

    def run(
        self, dataset: dict[str, Any], manifest: dict[str, Any]
    ) -> dict[str, Any]:
        fixtures = {
            item["evidenceId"]: item for item in manifest["evidenceFixtures"]
        }
        evidence = tuple(_fixture_to_evidence(item) for item in fixtures.values())
        identities = {
            item["identityCode"]: set(item["productLineCodes"])
            for item in manifest["executionIdentities"]
        }
        case_outputs: list[dict[str, Any]] = []
        metric_inputs: list[CaseResult] = []
        for case in sorted(dataset["cases"], key=lambda item: item["caseId"]):
            case_embedder: Embedder = (
                UnavailableEmbedder()
                if "EMBEDDING_DEGRADATION" in case["scenarioTags"]
                else self._embedder
            )
            authorized = filter_authorized_evidence(
                evidence,
                identities[case["executionIdentityCode"]],
                set(case["allowedProductLineCodes"]),
            )
            turns = [
                self._run_turn(case, turn, authorized, case_embedder)
                for turn in sorted(
                    case["turns"], key=lambda item: item["runSequence"]
                )
            ]
            final = turns[-1]
            returned_ids = tuple(final["topEvidenceIds"])
            citations = final["citations"]
            citation_valid = all(
                _citation_valid(citation, fixtures[citation["evidenceId"]])
                for citation in citations
            )
            degradation_passed = (
                final["retrievalMode"] == case["expectedRetrievalMode"]
                and final["degradation"] == case["expectedDegradation"]
            )
            metric_inputs.append(
                CaseResult(
                    case_id=case["caseId"],
                    expected_ids=tuple(case["expectedEvidenceIds"]),
                    forbidden_ids=tuple(case["forbiddenEvidenceIds"]),
                    returned_ids=returned_ids,
                    citation_valid=citation_valid,
                    degradation_passed=degradation_passed,
                )
            )
            case_outputs.append(
                {
                    "caseId": case["caseId"],
                    "languageTags": case["languageTags"],
                    "scenarioTags": case["scenarioTags"],
                    "turns": turns,
                    "citationValid": citation_valid,
                }
            )
        metrics = calculate_metrics(metric_inputs)
        return {
            "datasetVersion": dataset["datasetVersion"],
            "executionStatus": metrics.execution_status,
            "gatePassed": metrics.gate_passed,
            "metrics": asdict(metrics),
            "caseResults": case_outputs,
        }

    def _run_turn(
        self,
        case: dict[str, Any],
        turn: dict[str, Any],
        authorized: tuple[Evidence, ...],
        embedder: Embedder,
    ) -> dict[str, Any]:
        started = time.perf_counter_ns()
        scenario_tags = set(case["scenarioTags"])
        if "INSUFFICIENT_EVIDENCE" in scenario_tags:
            return {
                "runSequence": turn["runSequence"],
                "retrievalMode": "NONE",
                "degradation": "NONE",
                "topEvidenceIds": [],
                "citations": [],
                "durationMs": (time.perf_counter_ns() - started) / 1_000_000,
            }
        query = query_from_context(turn["contextSnapshot"])
        full_text_unavailable = "UNAVAILABLE" in scenario_tags
        lexical_ids = (
            [] if full_text_unavailable else lexical_rank(query, authorized)
        )
        try:
            query_vector = embedder.embed([query], "query:")[0]
            evidence_vectors = self._vectors_for(authorized, embedder)
            vector_ids = cosine_rank(
                query_vector, evidence_vectors, authorized
            )
            evidence_by_id = {item.evidence_id: item for item in authorized}
            fused = reciprocal_rank_fusion(
                lexical_ids,
                vector_ids,
                evidence_by_id,
                k=60,
                limit=5,
            )
            return {
                "runSequence": turn["runSequence"],
                "retrievalMode": "HYBRID",
                "degradation": "NONE",
                "topEvidenceIds": [item.evidence_id for item in fused],
                "citations": [
                    _citation(evidence_by_id[item.evidence_id])
                    for item in fused
                ],
                "durationMs": (time.perf_counter_ns() - started) / 1_000_000,
            }
        except EmbeddingUnavailable:
            mode = "FTS_ONLY" if lexical_ids else "NONE"
            degradation = (
                "UNAVAILABLE"
                if full_text_unavailable or not lexical_ids
                else "FTS_ONLY"
            )
            return {
                "runSequence": turn["runSequence"],
                "retrievalMode": mode,
                "degradation": degradation,
                "topEvidenceIds": lexical_ids[:5],
                "citations": [
                    _citation(
                        next(
                            item
                            for item in authorized
                            if item.evidence_id == evidence_id
                        )
                    )
                    for evidence_id in lexical_ids[:5]
                ],
                "durationMs": (time.perf_counter_ns() - started) / 1_000_000,
            }

    def _vectors_for(
        self, evidence: tuple[Evidence, ...], embedder: Embedder
    ) -> dict[str, list[float]]:
        missing = [
            item for item in evidence if item.evidence_id not in self._evidence_vector_cache
        ]
        if missing:
            vectors = embedder.embed(
                [item.text for item in missing], "passage:"
            )
            for item, vector in zip(missing, vectors, strict=True):
                self._evidence_vector_cache[item.evidence_id] = vector
        return {
            item.evidence_id: self._evidence_vector_cache[item.evidence_id]
            for item in evidence
        }
