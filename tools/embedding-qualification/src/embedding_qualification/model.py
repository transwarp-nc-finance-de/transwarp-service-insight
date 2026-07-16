from __future__ import annotations

import os
from pathlib import Path

from .qualification import EmbeddingUnavailable


class LocalE5Embedder:
    def __init__(self, model_dir: Path) -> None:
        model_dir = model_dir.resolve(strict=True)
        if not model_dir.is_absolute():
            raise ValueError("model directory must be absolute")
        os.environ["HF_HUB_OFFLINE"] = "1"
        os.environ["TRANSFORMERS_OFFLINE"] = "1"
        os.environ["HF_DATASETS_OFFLINE"] = "1"
        os.environ.setdefault("CUDA_VISIBLE_DEVICES", "")
        try:
            import torch
            from transformers import AutoModel, AutoTokenizer

            self._torch = torch
            thread_count = int(os.environ.get("OMP_NUM_THREADS", "1"))
            torch.set_num_threads(thread_count)
            self._tokenizer = AutoTokenizer.from_pretrained(
                str(model_dir),
                local_files_only=True,
                trust_remote_code=False,
                use_fast=True,
            )
            if not self._tokenizer.is_fast:
                raise RuntimeError("slow tokenizer is forbidden")
            self._model = AutoModel.from_pretrained(
                str(model_dir),
                local_files_only=True,
                trust_remote_code=False,
                use_safetensors=True,
            )
            self._model.to("cpu")
            self._model.eval()
            if next(self._model.parameters()).device.type != "cpu":
                raise RuntimeError("CPU-only qualification required")
        except Exception as error:
            raise EmbeddingUnavailable(
                f"local model load failed: {type(error).__name__}: {error}"
            ) from error

    def embed(self, texts: list[str], prefix: str) -> list[list[float]]:
        if prefix not in {"query:", "passage:"}:
            raise ValueError(f"unsupported E5 prefix: {prefix}")
        inputs = [f"{prefix} {text}" for text in texts]
        try:
            encoded = self._tokenizer(
                inputs,
                max_length=512,
                padding=True,
                truncation=True,
                return_tensors="pt",
            )
            with self._torch.inference_mode():
                output = self._model(**encoded)
                attention_mask = encoded["attention_mask"]
                masked = output.last_hidden_state.masked_fill(
                    ~attention_mask[..., None].bool(), 0.0
                )
                pooled = masked.sum(dim=1) / attention_mask.sum(dim=1)[
                    ..., None
                ]
                normalized = self._torch.nn.functional.normalize(
                    pooled, p=2, dim=1
                )
            vectors = normalized.cpu().tolist()
            if any(len(vector) != 768 for vector in vectors):
                raise RuntimeError("model output dimension is not 768")
            return vectors
        except Exception as error:
            raise EmbeddingUnavailable(
                f"local inference failed: {type(error).__name__}: {error}"
            ) from error

    def token_counts(self, texts: list[str], prefix: str) -> list[int]:
        encoded = self._tokenizer(
            [f"{prefix} {text}" for text in texts],
            max_length=512,
            padding=False,
            truncation=True,
        )
        return [len(ids) for ids in encoded["input_ids"]]
