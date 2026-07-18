from __future__ import annotations

import math
import re
from collections import Counter
from dataclasses import dataclass
from typing import Iterable, Mapping, Sequence


@dataclass(frozen=True)
class Evidence:
    evidence_id: str
    product_line_code: str
    version_id: str
    chunk_id: str
    text: str
    document_id: str = ""
    content_hash: str = ""


@dataclass(frozen=True)
class RankedEvidence:
    evidence_id: str
    score: float


def filter_authorized_evidence(
    evidence: Iterable[Evidence],
    identity_product_lines: set[str],
    case_product_lines: set[str],
) -> tuple[Evidence, ...]:
    allowed = identity_product_lines.intersection(case_product_lines)
    return tuple(item for item in evidence if item.product_line_code in allowed)


_TOKEN_PATTERN = re.compile(r"[\w.-]+", re.UNICODE)


def _tokens(text: str) -> Counter[str]:
    return Counter(token.lower() for token in _TOKEN_PATTERN.findall(text))


def lexical_rank(query: str, evidence: Sequence[Evidence]) -> list[str]:
    query_tokens = _tokens(query)
    scored: list[tuple[float, str, str, str]] = []
    for item in evidence:
        document_tokens = _tokens(item.text)
        overlap = sum(
            min(count, document_tokens[token])
            for token, count in query_tokens.items()
        )
        if overlap:
            scored.append(
                (
                    -float(overlap),
                    item.version_id,
                    item.chunk_id,
                    item.evidence_id,
                )
            )
    scored.sort()
    return [row[3] for row in scored[:20]]


def cosine_rank(
    query_vector: Sequence[float],
    evidence_vectors: Mapping[str, Sequence[float]],
    evidence: Sequence[Evidence],
) -> list[str]:
    scored: list[tuple[float, str, str, str]] = []
    for item in evidence:
        vector = evidence_vectors[item.evidence_id]
        score = sum(a * b for a, b in zip(query_vector, vector, strict=True))
        if not math.isfinite(score):
            raise ValueError(f"non-finite vector score: {item.evidence_id}")
        scored.append((-score, item.version_id, item.chunk_id, item.evidence_id))
    scored.sort()
    return [row[3] for row in scored[:20]]


def reciprocal_rank_fusion(
    lexical_ids: Sequence[str],
    vector_ids: Sequence[str],
    evidence_by_id: Mapping[str, Evidence],
    k: int = 60,
    limit: int = 5,
) -> tuple[RankedEvidence, ...]:
    scores: dict[str, float] = {}
    for ranking in (lexical_ids, vector_ids):
        for rank, evidence_id in enumerate(ranking, start=1):
            scores[evidence_id] = scores.get(evidence_id, 0.0) + 1.0 / (k + rank)
    ordered = sorted(
        scores,
        key=lambda evidence_id: (
            -scores[evidence_id],
            evidence_by_id[evidence_id].version_id,
            evidence_by_id[evidence_id].chunk_id,
        ),
    )
    return tuple(
        RankedEvidence(evidence_id, scores[evidence_id])
        for evidence_id in ordered[:limit]
    )
