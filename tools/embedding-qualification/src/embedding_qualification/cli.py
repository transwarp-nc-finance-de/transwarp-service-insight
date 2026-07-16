from __future__ import annotations

import argparse
import json
import time
import uuid
from pathlib import Path
from typing import Any

from .benchmark import (
    batch_benchmark,
    cgroup_memory,
    environment_summary,
    latency_benchmark,
    process_rss_bytes,
)
from .fetch import fetch_allowlisted_files
from .manifest import (
    build_manifest,
    load_allowlist,
    manifest_sha256,
    validate_artifact_directory,
)
from .model import LocalE5Embedder
from .provenance import QualificationProvenance, intelligent_output_context
from .qualification import QualificationRunner, query_from_context


def _read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def _load_provenance(path: Path) -> QualificationProvenance:
    return QualificationProvenance.from_dict(_read_json(path))


def command_fetch(args: argparse.Namespace) -> None:
    allowlist_payload = _read_json(args.allowlist)
    entries = load_allowlist(args.allowlist)
    records = fetch_allowlisted_files(
        args.model_dir,
        entries,
        allowlist_payload["modelId"],
        allowlist_payload["revision"],
    )
    args.evidence_dir.mkdir(parents=True, exist_ok=True)
    _write_json(args.evidence_dir / "download-records.json", records)
    manifest = build_manifest(args.model_dir, entries)
    (args.evidence_dir / "model.manifest").write_text(
        manifest, encoding="utf-8", newline="\n"
    )
    (args.evidence_dir / "model.manifest.sha256").write_text(
        manifest_sha256(manifest) + "\n", encoding="ascii", newline="\n"
    )


def command_verify(args: argparse.Namespace) -> None:
    entries = load_allowlist(args.allowlist)
    validate_artifact_directory(args.model_dir, entries)
    manifest = build_manifest(args.model_dir, entries)
    expected = args.manifest.read_text(encoding="utf-8")
    if manifest != expected:
        raise ValueError("model manifest mismatch")
    print(manifest_sha256(manifest))


def command_qualify(args: argparse.Namespace) -> None:
    dataset_path = args.dataset_dir / "dataset.json"
    evidence_path = args.dataset_dir / "evidence-fixture-manifest.json"
    dataset = _read_json(dataset_path)
    evidence = _read_json(evidence_path)
    embedder = LocalE5Embedder(args.model_dir)
    result = QualificationRunner(embedder).run(dataset, evidence)
    provenance = _load_provenance(args.provenance)
    result.update(
        {
            "evaluationRunId": str(uuid.uuid4()),
            "provenance": provenance.to_dict(),
            **intelligent_output_context(),
        }
    )
    _write_json(args.output, result)


def command_latency(args: argparse.Namespace) -> None:
    dataset = _read_json(args.dataset_dir / "dataset.json")
    queries = [
        {
            "caseId": case["caseId"],
            "languageTags": case["languageTags"],
            "scenarioTags": case["scenarioTags"],
            "query": query_from_context(case["turns"][-1]["contextSnapshot"]),
        }
        for case in sorted(dataset["cases"], key=lambda item: item["caseId"])
    ]
    embedder = LocalE5Embedder(args.model_dir)
    provenance = _load_provenance(args.provenance)
    output = latency_benchmark(
        embedder,
        queries,
        model_manifest_hash=provenance.model_manifest_hash,
        warmups=args.warmups,
        iterations=args.iterations,
        rounds=args.rounds,
    )
    output["environment"] = environment_summary()
    output["provenance"] = provenance.to_dict()
    output.update(intelligent_output_context())
    _write_json(args.output, output)


def command_smoke(args: argparse.Namespace) -> None:
    provenance = _load_provenance(args.provenance)
    baseline = process_rss_bytes()
    load_started = time.perf_counter()
    embedder = LocalE5Embedder(args.model_dir)
    load_seconds = time.perf_counter() - load_started
    loaded = process_rss_bytes()
    inference_started = time.perf_counter()
    vector = embedder.embed(["模拟数据：offline CPU smoke test"], "query:")[0]
    inference_ms = (time.perf_counter() - inference_started) * 1000
    first_inference = process_rss_bytes()
    time.sleep(1)
    output = {
        "baselineRssBytes": baseline,
        "loadedRssBytes": loaded,
        "firstInferenceRssBytes": first_inference,
        "stableIdleRssBytes": process_rss_bytes(),
        "loadSeconds": load_seconds,
        "firstInferenceMs": inference_ms,
        "vectorDimensions": len(vector),
        "cpuOnly": True,
        "cgroup": cgroup_memory(),
        "environment": environment_summary(),
        "provenance": provenance.to_dict(),
        **intelligent_output_context(),
    }
    _write_json(args.output, output)


def command_batch(args: argparse.Namespace) -> None:
    provenance = _load_provenance(args.provenance)
    evidence = _read_json(
        args.dataset_dir / "evidence-fixture-manifest.json"
    )
    fixture_texts = [
        f"{item['document']['title']} {item['excerpt']}"
        for item in evidence["evidenceFixtures"]
    ]
    embedder = LocalE5Embedder(args.model_dir)
    output = batch_benchmark(
        embedder,
        fixture_texts,
        counts=args.counts,
        token_buckets=args.token_buckets,
        batch_sizes=args.batch_sizes,
    )
    output["environment"] = environment_summary()
    output["cgroup"] = cgroup_memory()
    output["provenance"] = provenance.to_dict()
    output.update(intelligent_output_context())
    _write_json(args.output, output)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Issue #39 isolated embedding qualification harness"
    )
    subparsers = parser.add_subparsers(required=True)
    fetch = subparsers.add_parser("fetch")
    fetch.add_argument("--allowlist", type=Path, required=True)
    fetch.add_argument("--model-dir", type=Path, required=True)
    fetch.add_argument("--evidence-dir", type=Path, required=True)
    fetch.set_defaults(handler=command_fetch)
    verify = subparsers.add_parser("verify")
    verify.add_argument("--allowlist", type=Path, required=True)
    verify.add_argument("--model-dir", type=Path, required=True)
    verify.add_argument("--manifest", type=Path, required=True)
    verify.set_defaults(handler=command_verify)
    qualify = subparsers.add_parser("qualify")
    qualify.add_argument("--model-dir", type=Path, required=True)
    qualify.add_argument("--dataset-dir", type=Path, required=True)
    qualify.add_argument("--output", type=Path, required=True)
    qualify.add_argument("--provenance", type=Path, required=True)
    qualify.set_defaults(handler=command_qualify)
    latency = subparsers.add_parser("latency")
    latency.add_argument("--model-dir", type=Path, required=True)
    latency.add_argument("--dataset-dir", type=Path, required=True)
    latency.add_argument("--output", type=Path, required=True)
    latency.add_argument("--warmups", type=int, default=100)
    latency.add_argument("--iterations", type=int, default=1000)
    latency.add_argument("--rounds", type=int, default=3)
    latency.add_argument("--provenance", type=Path, required=True)
    latency.set_defaults(handler=command_latency)
    smoke = subparsers.add_parser("smoke")
    smoke.add_argument("--model-dir", type=Path, required=True)
    smoke.add_argument("--output", type=Path, required=True)
    smoke.add_argument("--provenance", type=Path, required=True)
    smoke.set_defaults(handler=command_smoke)
    batch = subparsers.add_parser("batch")
    batch.add_argument("--model-dir", type=Path, required=True)
    batch.add_argument("--dataset-dir", type=Path, required=True)
    batch.add_argument("--output", type=Path, required=True)
    batch.add_argument("--provenance", type=Path, required=True)
    batch.add_argument(
        "--counts", type=int, nargs="+", default=[100, 1000, 10000]
    )
    batch.add_argument(
        "--token-buckets", type=int, nargs="+", default=[32, 128, 512]
    )
    batch.add_argument(
        "--batch-sizes", type=int, nargs="+", default=[1, 8, 16, 32]
    )
    batch.set_defaults(handler=command_batch)
    return parser


def main() -> None:
    args = build_parser().parse_args()
    args.handler(args)


if __name__ == "__main__":
    main()
