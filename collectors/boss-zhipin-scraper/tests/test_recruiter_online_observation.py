import csv
import importlib.util
import json
import pathlib
import sys
import tempfile
import unittest
from datetime import datetime
from unittest import mock


ROOT_PATH = pathlib.Path(__file__).resolve().parents[1]
SCRIPT_PATH = ROOT_PATH / "scripts" / "boss_cdp_raw.py"


def load_module():
    sys.modules.setdefault("websocket", mock.Mock())
    sys.modules.setdefault("requests", mock.Mock())
    spec = importlib.util.spec_from_file_location(
        "boss_cdp_raw_recruiter_online",
        SCRIPT_PATH,
    )
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class RecruiterOnlineObservationTests(unittest.TestCase):
    def test_xhr_template_reads_boss_online_only_as_boolean(self):
        module = load_module()

        self.assertIn(
            "typeof j.bossOnline === 'boolean' ? j.bossOnline : null",
            module.FETCH_API_JS_TEMPLATE,
        )
        self.assertIn("boss_online: bossOnline", module.FETCH_API_JS_TEMPLATE)

    def test_current_observation_time_is_explicit_utc_timestamp(self):
        module = load_module()

        observed_at = module.current_utc_observation_time()
        parsed = datetime.fromisoformat(observed_at.replace("Z", "+00:00"))

        self.assertTrue(observed_at.endswith("Z"))
        self.assertIsNotNone(parsed.tzinfo)
        self.assertEqual(parsed.utcoffset().total_seconds(), 0)

    def test_only_true_value_receives_shared_observation_time(self):
        module = load_module()
        observed_at = "2026-07-27T05:30:00.123Z"
        jobs = [
            {"title": "A", "boss_online": True},
            {"title": "B", "boss_online": True},
            {"title": "C", "boss_online": False},
            {"title": "D"},
            {"title": "E", "boss_online": None},
            {"title": "F", "boss_online": "true"},
        ]
        original = json.loads(json.dumps(jobs))

        enriched = module.add_recruiter_online_observation(jobs, observed_at)

        self.assertEqual(enriched[0]["boss_online_observed_at"], observed_at)
        self.assertEqual(enriched[1]["boss_online_observed_at"], observed_at)
        for item in enriched[2:]:
            self.assertIsNone(item["boss_online_observed_at"])
        self.assertFalse(enriched[2]["boss_online"])
        for item in enriched[3:]:
            self.assertIsNone(item["boss_online"])
        self.assertEqual(jobs, original)

    def test_enriched_jobs_remain_json_and_csv_serializable(self):
        module = load_module()
        observed_at = "2026-07-27T05:30:00.123Z"
        enriched = module.add_recruiter_online_observation(
            [{"title": "Java", "boss_online": True}],
            observed_at,
        )

        self.assertIn(observed_at, json.dumps(enriched))
        with tempfile.TemporaryDirectory() as tmp:
            csv_path = pathlib.Path(tmp) / "jobs.csv"
            module.write_csv(csv_path, enriched)
            with csv_path.open(encoding="utf-8-sig", newline="") as stream:
                rows = list(csv.DictReader(stream))

        self.assertEqual(rows[0]["title"], "Java")


if __name__ == "__main__":
    unittest.main()
