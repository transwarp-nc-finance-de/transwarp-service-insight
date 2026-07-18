import unittest

from embedding_qualification.fetch import curl_fetch_command


class FetchTest(unittest.TestCase):
    def test_curl_disables_user_configuration_before_other_arguments(self) -> None:
        command = curl_fetch_command(
            "C:/tools/curl.exe",
            "C:/artifacts/model.part",
            "https://example.invalid/model",
        )

        self.assertEqual(command[:2], ["C:/tools/curl.exe", "--disable"])
        self.assertIn("--proto", command)
        self.assertIn("=https", command)

if __name__ == "__main__":
    unittest.main()
