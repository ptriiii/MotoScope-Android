import unittest
import tempfile
from unittest.mock import patch
from urllib.error import HTTPError

import honda360


class HondaBlockingPropagationTests(unittest.TestCase):
    def test_fetch_turns_blocking_http_error_into_distinct_signal(self) -> None:
        blocked = HTTPError("https://example.test", 403, "Forbidden", {}, None)
        with patch.object(honda360.shutil, "which", return_value=None):
            with patch.object(honda360, "urlopen", side_effect=blocked):
                with self.assertRaises(honda360.HondaAccessBlockedError):
                    honda360.fetch("https://example.test", delay=0, retries=3)

    def test_main_stops_batch_on_blocking_signal(self) -> None:
        entries = [
            {"slug": "first", "url": "https://example.test/first"},
            {"slug": "second", "url": "https://example.test/second"},
        ]
        calls: list[str] = []

        def blocked(entry: dict[str, str], *args: object) -> dict[str, object]:
            calls.append(entry["slug"])
            raise honda360.HondaAccessBlockedError("HTTP 429")

        with tempfile.TemporaryDirectory() as directory:
            with patch.object(honda360, "discover_catalog", return_value=entries):
                with patch.object(honda360, "archive_sequence", side_effect=blocked):
                    result = honda360.main(["--batch", "--output", directory])
        self.assertEqual(result, 3)
        self.assertEqual(calls, ["first"])


if __name__ == "__main__":
    unittest.main()
