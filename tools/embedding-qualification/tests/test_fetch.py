import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from embedding_qualification.fetch import (
    curl_fetch_command,
    curl_version_command,
    qualification_curl,
)


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

    def test_version_command_also_disables_user_configuration(self) -> None:
        self.assertEqual(
            curl_version_command("C:/tools/curl.exe"),
            ["C:/tools/curl.exe", "--disable", "--version"],
        )

    def test_curl_requires_configured_absolute_existing_path(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(ValueError, "QUALIFICATION_CURL"):
                qualification_curl()

        with patch.dict(os.environ, {"QUALIFICATION_CURL": "curl"}):
            with self.assertRaisesRegex(ValueError, "absolute"):
                qualification_curl()

        with tempfile.TemporaryDirectory() as directory:
            executable = Path(directory) / "curl"
            executable.write_bytes(b"reviewed executable")
            with patch.dict(
                os.environ,
                {"QUALIFICATION_CURL": str(executable.resolve())},
            ):
                self.assertEqual(qualification_curl(), str(executable.resolve()))


if __name__ == "__main__":
    unittest.main()
