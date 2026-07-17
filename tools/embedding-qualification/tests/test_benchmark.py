import unittest
from unittest.mock import patch

from embedding_qualification.benchmark import batch_benchmark


class FakeMemorySampler:
    peak_rss_bytes = 123

    def __enter__(self) -> "FakeMemorySampler":
        return self

    def __exit__(self, *args: object) -> None:
        return None


class FakeEmbedder:
    def fit_to_token_count(self, text: str, prefix: str, target: int) -> str:
        del text, prefix
        return " ".join(["token"] * target)

    def token_counts(self, texts: list[str], prefix: str) -> list[int]:
        del prefix
        return [len(text.split()) for text in texts]

    def embed(self, texts: list[str], prefix: str) -> list[list[float]]:
        del prefix
        return [[0.0] * 768 for _ in texts]


class BatchBenchmarkTest(unittest.TestCase):
    def test_generated_chunks_match_each_target_token_bucket(self) -> None:
        with patch(
            "embedding_qualification.benchmark.MemorySampler",
            FakeMemorySampler,
        ):
            result = batch_benchmark(
                FakeEmbedder(),
                ["模拟 Evidence 模板"],
                counts=[3],
                token_buckets=[32, 128],
                batch_sizes=[1, 3],
            )

        self.assertEqual(len(result["results"]), 4)
        for measurement in result["results"]:
            target = measurement["targetTokenBucket"]
            self.assertEqual(measurement["minTokenCount"], target)
            self.assertEqual(measurement["maxTokenCount"], target)
            self.assertEqual(
                measurement["totalTokens"],
                measurement["chunkCount"] * target,
            )


if __name__ == "__main__":
    unittest.main()
