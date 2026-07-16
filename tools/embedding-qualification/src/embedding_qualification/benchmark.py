from __future__ import annotations

import platform
import statistics
import threading
import time
from contextlib import AbstractContextManager
from pathlib import Path
from typing import Any

from .model import LocalE5Embedder


def percentile(values: list[float], quantile: float) -> float:
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, int(len(ordered) * quantile) - 1))
    return ordered[index]


def latency_benchmark(
    embedder: LocalE5Embedder,
    queries: list[dict[str, Any]],
    model_manifest_hash: str,
    warmups: int = 100,
    iterations: int = 1000,
    rounds: int = 3,
) -> dict[str, Any]:
    if not queries:
        raise ValueError("benchmark queries are required")
    for index in range(warmups):
        embedder.embed([queries[index % len(queries)]["query"]], "query:")
    round_outputs = []
    for round_number in range(1, rounds + 1):
        durations = []
        records = []
        for index in range(iterations):
            query_record = queries[index % len(queries)]
            query = query_record["query"]
            started = time.perf_counter_ns()
            embedder.embed([query], "query:")
            duration_ms = (time.perf_counter_ns() - started) / 1_000_000
            durations.append(duration_ms)
            records.append(
                {
                    "caseId": query_record["caseId"],
                    "languageTags": query_record["languageTags"],
                    "scenarioTags": query_record["scenarioTags"],
                    "tokenCount": embedder.token_counts([query], "query:")[0],
                    "durationMs": duration_ms,
                    "status": "SUCCEEDED",
                    "degradation": "NONE",
                    "modelManifestHash": model_manifest_hash,
                }
            )
        round_outputs.append(
            {
                "round": round_number,
                "p50Ms": statistics.median(durations),
                "p95Ms": percentile(durations, 0.95),
                "p99Ms": percentile(durations, 0.99),
                "maxMs": max(durations),
                "records": records,
            }
        )
    return {
        "warmups": warmups,
        "iterationsPerRound": iterations,
        "concurrency": 1,
        "batchSize": 1,
        "rounds": round_outputs,
    }


def environment_summary() -> dict[str, object]:
    return {
        "python": platform.python_version(),
        "platform": platform.platform(),
        "processor": platform.processor(),
    }


class MemorySampler(AbstractContextManager["MemorySampler"]):
    def __init__(self, interval_seconds: float = 0.05) -> None:
        import psutil

        self._process = psutil.Process()
        self._interval_seconds = interval_seconds
        self._stop = threading.Event()
        self._thread = threading.Thread(target=self._sample, daemon=True)
        self.peak_rss_bytes = self._process.memory_info().rss

    def _sample(self) -> None:
        while not self._stop.wait(self._interval_seconds):
            self.peak_rss_bytes = max(
                self.peak_rss_bytes, self._process.memory_info().rss
            )

    def __enter__(self) -> "MemorySampler":
        self._thread.start()
        return self

    def __exit__(self, *args: object) -> None:
        self._stop.set()
        self._thread.join()
        self.peak_rss_bytes = max(
            self.peak_rss_bytes, self._process.memory_info().rss
        )


def process_rss_bytes() -> int:
    import psutil

    return psutil.Process().memory_info().rss


def cgroup_memory() -> dict[str, int | None]:
    def read_value(name: str) -> int | None:
        try:
            value = Path(f"/sys/fs/cgroup/{name}").read_text(
                encoding="ascii"
            ).strip()
            return None if value == "max" else int(value)
        except (FileNotFoundError, ValueError):
            return None

    return {
        "currentBytes": read_value("memory.current"),
        "peakBytes": read_value("memory.peak"),
        "limitBytes": read_value("memory.max"),
    }


def batch_benchmark(
    embedder: LocalE5Embedder,
    fixture_texts: list[str],
    counts: list[int],
    token_buckets: list[int],
    batch_sizes: list[int],
) -> dict[str, Any]:
    outputs = []
    for count in counts:
        for token_bucket in token_buckets:
            sample = [
                (
                    f"{fixture_texts[index % len(fixture_texts)]} "
                    f"{'mock ' * token_bucket}unique-{index}"
                )
                for index in range(count)
            ]
            for batch_size in batch_sizes:
                durations: list[float] = []
                token_total = 0
                failures = 0
                started = time.perf_counter()
                with MemorySampler() as memory:
                    for offset in range(0, count, batch_size):
                        batch = sample[offset : offset + batch_size]
                        token_total += sum(
                            embedder.token_counts(batch, "passage:")
                        )
                        batch_started = time.perf_counter()
                        try:
                            vectors = embedder.embed(batch, "passage:")
                            if any(len(vector) != 768 for vector in vectors):
                                raise RuntimeError("invalid vector dimension")
                        except Exception:
                            failures += len(batch)
                            raise
                        durations.append(
                            (time.perf_counter() - batch_started) * 1000
                        )
                elapsed = time.perf_counter() - started
                outputs.append(
                    {
                        "chunkCount": count,
                        "targetTokenBucket": token_bucket,
                        "batchSize": batch_size,
                        "totalTokens": token_total,
                        "durationSeconds": elapsed,
                        "chunksPerSecond": count / elapsed,
                        "tokensPerSecond": token_total / elapsed,
                        "p95BatchLatencyMs": percentile(durations, 0.95),
                        "peakRssBytes": memory.peak_rss_bytes,
                        "failureCount": failures,
                        "finalVectorCount": count - failures,
                    }
                )
    return {"mockData": True, "results": outputs}
