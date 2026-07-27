import unittest

from integrations.boss_job_merger import (
    extract_details,
    merge_jobs_with_details,
)


class BossJobMergerTests(unittest.TestCase):
    def test_ninety_jobs_with_twenty_five_details_remains_left_join(self):
        jobs = [
            {
                "job_id": f"local-{index}",
                "encrypt_job_id": f"source-{index}",
                "title": f"Job {index}",
            }
            for index in range(90)
        ]
        details = [
            {
                "job_id": f"local-{index}",
                "title": f"Job {index}",
                "jd": f"JD {index}",
            }
            for index in range(25)
        ]

        merged = merge_jobs_with_details(jobs, details)

        self.assertEqual(len(merged), 90)
        self.assertEqual(
            sum(item.detail is not None for item in merged),
            25,
        )
        self.assertIsNone(merged[25].detail)

    def test_detail_root_object_is_supported(self):
        details = [{"job_id": "local-1", "jd": "JD"}]

        self.assertEqual(
            extract_details({"run_id": "run-1", "details": details}),
            details,
        )

    def test_duplicate_orphan_and_conflict_are_warned_without_values(self):
        jobs = [
            {
                "job_id": "local-1",
                "title": "List title",
                "salary": "20-30K",
            }
        ]
        details = [
            {
                "job_id": "local-1",
                "title": "First detail title",
                "jd": "sensitive first JD",
            },
            {
                "job_id": "local-1",
                "title": "Second detail title",
                "jd": "sensitive second JD",
            },
            {"job_id": "orphan", "jd": "orphan sensitive JD"},
        ]

        with self.assertLogs(
            "integrations.boss_job_merger",
            level="WARNING",
        ) as captured:
            merged = merge_jobs_with_details(jobs, details)

        logs = "\n".join(captured.output)
        self.assertIn("Duplicate BOSS detail job_id=local-1", logs)
        self.assertIn("Ignoring unmatched BOSS detail job_id=orphan", logs)
        self.assertIn("field=title", logs)
        self.assertNotIn("sensitive", logs)
        self.assertEqual(merged[0].detail["jd"], "sensitive second JD")
        self.assertEqual(merged[0].list_item["title"], "List title")

    def test_invalid_job_and_detail_shapes_fail_explicitly(self):
        with self.assertRaisesRegex(ValueError, "jobs must be an array"):
            merge_jobs_with_details({}, [])
        with self.assertRaisesRegex(ValueError, "details must be an array"):
            merge_jobs_with_details([], {"details": {}})
        with self.assertRaisesRegex(ValueError, "must be an object"):
            merge_jobs_with_details(["invalid"], [])


if __name__ == "__main__":
    unittest.main()
