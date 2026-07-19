from __future__ import annotations

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from embedding_service.manifest import verify_model

MODEL_ID = "intfloat/multilingual-e5-base"
MODEL_REVISION = "d13f1b27baf31030b7fd040960d60d909913633f"


class Application:
    def __init__(self) -> None:
        os.environ.update(HF_HUB_OFFLINE="1", TRANSFORMERS_OFFLINE="1", HF_DATASETS_OFFLINE="1", CUDA_VISIBLE_DEVICES="")
        model_dir = Path(os.environ.get("MODEL_DIR", "/model"))
        verify_model(model_dir, Path("/app/allowlist.json"))
        import torch
        from transformers import AutoModel, AutoTokenizer

        torch.set_num_threads(int(os.environ.get("OMP_NUM_THREADS", "1")))
        self.torch = torch
        self.tokenizer = AutoTokenizer.from_pretrained(
            str(model_dir), local_files_only=True, trust_remote_code=False, use_fast=True
        )
        if not self.tokenizer.is_fast:
            raise RuntimeError("slow tokenizer is forbidden")
        self.model = AutoModel.from_pretrained(
            str(model_dir), local_files_only=True, trust_remote_code=False, use_safetensors=True
        ).to("cpu")
        self.model.eval()

    def embed(self, texts: list[str], prefix: str) -> list[list[float]]:
        if prefix not in {"query", "passage"} or not texts or len(texts) > 128:
            raise ValueError("prefix must be query/passage and texts must contain 1..128 items")
        encoded = self.tokenizer(
            [f"{prefix}: {text}" for text in texts], max_length=512, padding=True,
            truncation=True, return_tensors="pt"
        )
        with self.torch.inference_mode():
            output = self.model(**encoded)
            mask = encoded["attention_mask"]
            masked = output.last_hidden_state.masked_fill(~mask[..., None].bool(), 0.0)
            pooled = masked.sum(dim=1) / mask.sum(dim=1)[..., None]
            vectors = self.torch.nn.functional.normalize(pooled, p=2, dim=1).cpu().tolist()
        if any(len(vector) != 768 for vector in vectors):
            raise RuntimeError("model output dimension is not 768")
        return vectors


APP = Application()


class Handler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        if self.path != "/health":
            return self._reply(404, {"error": "not found"})
        self._reply(200, {"status": "UP", "modelId": MODEL_ID, "revision": MODEL_REVISION})

    def do_POST(self) -> None:
        if self.path != "/v1/embeddings":
            return self._reply(404, {"error": "not found"})
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > 5_000_000:
                raise ValueError("invalid content length")
            payload = json.loads(self.rfile.read(length))
            self._reply(200, {"vectors": APP.embed(payload.get("texts"), payload.get("prefix")), "dimensions": 768})
        except (TypeError, ValueError, json.JSONDecodeError) as error:
            self._reply(400, {"error": str(error)})
        except Exception:
            self._reply(503, {"error": "local embedding inference unavailable"})

    def log_message(self, format: str, *args: object) -> None:
        return

    def _reply(self, status: int, payload: dict[str, object]) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", 8090), Handler).serve_forever()
