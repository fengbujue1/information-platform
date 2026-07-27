import copy
import json
import unittest

from integrations import BossMappingError, MapperConfig, map_boss_results


class InformationMapperTests(unittest.TestCase):
    def test_normal_job_maps_to_information_envelope_v1(self):
        root = list_root()
        details = {
            "run_id": "detail-run",
            "details": [
                {
                    "job_id": "local-1",
                    "title": "Java开发工程师",
                    "company": "示例公司",
                    "salary": "20-35K·13薪",
                    "location": "成都·武侯区·中和",
                    "tags_list": "3-5年 | 本科 | 自定义标签",
                    "job_link": "https://example.test/job/source-1",
                    "detail_status": "FETCHED",
                    "detail_collected_at": "2026-07-22T23:14:10+08:00",
                    "skill_tags": ["not-standardized"],
                    "jd": "完整 JD",
                }
            ],
        }
        config = MapperConfig(run_id_factory=lambda: "mapper-run")

        envelope = map_boss_results(root, details, config)[0]

        self.assertEqual(envelope["schemaVersion"], 1)
        self.assertEqual(envelope["informationType"], "JOB")
        self.assertEqual(envelope["source"], "BOSS")
        self.assertEqual(envelope["sourceItemId"], "source-1")
        self.assertNotIn("job_id", envelope)
        self.assertEqual(envelope["title"], "Java开发工程师")
        self.assertEqual(envelope["content"], "完整 JD")
        self.assertIsNone(envelope["publishTime"])
        self.assertEqual(envelope["collectedAt"], "2026-07-22T15:14:09.656461Z")
        self.assertEqual(envelope["collector"]["collectorVersion"], "2.1.0")
        self.assertEqual(envelope["collectionContext"]["runId"], "mapper-run")
        self.assertEqual(
            envelope["collectionContext"]["sourceScrapedAt"],
            "2026-07-22T23:14:09.656461+08:00",
        )
        extension = envelope["extension"]
        self.assertEqual(extension["sourceCompanyId"], "brand-1")
        self.assertEqual(extension["sourceRecruiterId"], "boss-1")
        self.assertEqual(extension["companyName"], "示例公司")
        self.assertIsNone(extension["recruiterName"])
        self.assertEqual(extension["recruiterTitle"], "招聘经理")
        self.assertEqual(extension["salaryMinMonthlyYuan"], 20000)
        self.assertEqual(extension["salaryMaxMonthlyYuan"], 35000)
        self.assertEqual(extension["salaryMonths"], 13)
        self.assertEqual(extension["cityName"], "成都")
        self.assertEqual(extension["areaName"], "武侯区")
        self.assertEqual(extension["businessDistrictName"], "中和")
        self.assertEqual(extension["experienceText"], "3-5年")
        self.assertEqual(extension["educationText"], "本科")
        self.assertEqual(extension["sourceTags"], ["3-5年", "本科", "自定义标签"])
        self.assertEqual(
            extension["sourceSkillTags"],
            ["Java", "Spring Boot", "不接受居家办公"],
        )
        self.assertEqual(extension["welfare"], ["五险一金", "带薪年假"])
        self.assertEqual(extension["detailStatus"], "FETCHED")
        self.assertEqual(
            extension["detailCollectedAt"],
            "2026-07-22T23:14:10+08:00",
        )
        self.assertEqual(
            envelope["rawPayload"]["detail"]["skill_tags"],
            ["not-standardized"],
        )
        self.assertEqual(
            envelope["rawPayload"]["list"]["job_labels"],
            "3-5年 | 本科",
        )
        self.assertNotIn("jobLabels", extension)
        json.dumps(envelope, ensure_ascii=False)

    def test_missing_details_still_produce_unknown_detail_envelope(self):
        envelope = map_boss_results(
            list_root(),
            [],
            MapperConfig(run_id_factory=lambda: "run"),
        )[0]

        self.assertIsNone(envelope["content"])
        self.assertEqual(envelope["extension"]["detailStatus"], "UNKNOWN")
        self.assertIsNone(envelope["extension"]["detailCollectedAt"])
        self.assertIsNone(envelope["rawPayload"]["detail"])

    def test_ninety_jobs_and_twenty_five_details_map_to_one_shared_batch(self):
        root = list_root()
        root["jobs"] = []
        for index in range(90):
            item = copy.deepcopy(list_root()["jobs"][0])
            item["job_id"] = f"local-{index}"
            item["encrypt_job_id"] = f"source-{index}"
            item["title"] = f"Job {index}"
            root["jobs"].append(item)
        root["total"] = 90
        details = [
            {
                "job_id": f"local-{index}",
                "title": f"Job {index}",
                "jd": f"JD {index}",
            }
            for index in range(25)
        ]

        envelopes = map_boss_results(
            root,
            details,
            MapperConfig(run_id_factory=lambda: "shared-run"),
        )

        self.assertEqual(len(envelopes), 90)
        self.assertEqual(
            sum(envelope["content"] is not None for envelope in envelopes),
            25,
        )
        self.assertEqual(
            {envelope["collectionContext"]["runId"] for envelope in envelopes},
            {"shared-run"},
        )
        self.assertEqual(
            {envelope["sourceItemId"] for envelope in envelopes},
            {f"source-{index}" for index in range(90)},
        )
    def test_location_empty_ids_arrays_and_unrecognized_tags_are_safe(self):
        root = list_root(
            job_overrides={
                "encrypt_brand_id": " ",
                "location": "成都··",
                "tags": "自定义 | 未知学历",
                "skills": "",
                "welfare": "五险一金 | | 五险一金 | 带薪年假",
            }
        )

        extension = map_boss_results(root, [])[0]["extension"]

        self.assertIsNone(extension["sourceCompanyId"])
        self.assertEqual(extension["locationName"], "成都··")
        self.assertEqual(extension["cityName"], "成都")
        self.assertIsNone(extension["areaName"])
        self.assertIsNone(extension["businessDistrictName"])
        self.assertEqual(extension["sourceTags"], ["自定义", "未知学历"])
        self.assertIsNone(extension["experienceText"])
        self.assertIsNone(extension["educationText"])
        self.assertIsNone(extension["sourceSkillTags"])
        self.assertEqual(extension["welfare"], ["五险一金", "带薪年假"])

    def test_salary_parses_thirteen_through_sixteen_months(self):
        for months in range(13, 17):
            with self.subTest(months=months):
                root = list_root(
                    job_overrides={"salary": f"15-30K·{months}薪"}
                )
                extension = map_boss_results(root, [])[0]["extension"]
                self.assertEqual(extension["salaryMinMonthlyYuan"], 15000)
                self.assertEqual(extension["salaryMaxMonthlyYuan"], 30000)
                self.assertEqual(extension["salaryMonths"], months)

        unknown = map_boss_results(
            list_root(job_overrides={"salary": "面议"}),
            [],
        )[0]["extension"]
        self.assertEqual(unknown["salaryText"], "面议")
        self.assertIsNone(unknown["salaryMinMonthlyYuan"])
        self.assertIsNone(unknown["salaryMaxMonthlyYuan"])
        self.assertIsNone(unknown["salaryMonths"])

    def test_historical_timezone_is_explicit_and_configurable(self):
        root = list_root(scraped_at="2026-07-22T23:14:09")

        envelope = map_boss_results(
            root,
            [],
            MapperConfig(run_id_factory=lambda: "run"),
        )[0]

        self.assertEqual(envelope["collectedAt"], "2026-07-22T15:14:09Z")
        self.assertEqual(
            envelope["collectionContext"]["sourceScrapedAt"],
            "2026-07-22T23:14:09+08:00",
        )
        self.assertEqual(
            envelope["collectionContext"]["timeZoneAssumption"],
            "Asia/Shanghai",
        )

        utc_envelope = map_boss_results(
            root,
            [],
            MapperConfig(
                historical_timezone="UTC",
                run_id_factory=lambda: "run",
            ),
        )[0]
        self.assertEqual(utc_envelope["collectedAt"], "2026-07-22T23:14:09Z")

    def test_recruiter_observation_maps_only_strict_true_with_timezone(self):
        observed_at = "2026-07-27T05:30:00.123Z"
        valid = map_boss_results(
            list_root(
                job_overrides={
                    "boss_online": True,
                    "boss_online_observed_at": observed_at,
                }
            ),
            [],
        )[0]
        self.assertEqual(
            valid["extension"]["recruiterActiveText"],
            observed_at,
        )
        self.assertTrue(valid["rawPayload"]["list"]["boss_online"])
        self.assertEqual(
            valid["rawPayload"]["list"]["boss_online_observed_at"],
            observed_at,
        )

        for online_value in (False, None, "true"):
            with self.subTest(online_value=online_value):
                envelope = map_boss_results(
                    list_root(
                        job_overrides={
                            "boss_online": online_value,
                            "boss_online_observed_at": observed_at,
                        }
                    ),
                    [],
                )[0]
                self.assertIsNone(
                    envelope["extension"]["recruiterActiveText"]
                )
                self.assertIs(
                    envelope["rawPayload"]["list"]["boss_online"],
                    online_value,
                )

        missing_root = list_root()
        missing_root["jobs"][0].pop("boss_online")
        missing_root["jobs"][0].pop("boss_online_observed_at")
        missing = map_boss_results(missing_root, [])[0]
        self.assertIsNone(missing["extension"]["recruiterActiveText"])
        self.assertNotIn("boss_online", missing["rawPayload"]["list"])
        with self.assertLogs(
            "integrations.information_mapper",
            level="WARNING",
        ):
            invalid = map_boss_results(
                list_root(
                    job_overrides={
                        "boss_online": True,
                        "boss_online_observed_at": "2026-07-27T05:30:00",
                    }
                ),
                [],
            )[0]
        self.assertIsNone(invalid["extension"]["recruiterActiveText"])

    def test_raw_payload_is_recursively_sanitized_without_mutating_input(self):
        root = list_root(
            job_overrides={
                "security_id": "must-not-leave",
                "nested": {
                    "Li-D": "must-not-leave",
                    "Cookie": "must-not-leave",
                    "safe": [{"access_token": "must-not-leave"}, "kept"],
                },
            }
        )
        details = [
            detail(
                overrides={
                    "security-id": "must-not-leave",
                    "browser_credentials": "must-not-leave",
                }
            )
        ]
        original_root = copy.deepcopy(root)
        original_details = copy.deepcopy(details)

        raw_payload = map_boss_results(root, details)[0]["rawPayload"]
        serialized = json.dumps(raw_payload, ensure_ascii=False)

        self.assertNotIn("must-not-leave", serialized)
        self.assertEqual(raw_payload["list"]["nested"]["safe"], [{}, "kept"])
        self.assertEqual(root, original_root)
        self.assertEqual(details, original_details)

    def test_explicit_failure_status_is_preserved_without_jd(self):
        failed_detail = detail(
            overrides={
                "jd": "",
                "detail_status": "FAILED",
                "detail_collected_at": None,
            }
        )

        extension = map_boss_results(list_root(), [failed_detail])[0]["extension"]

        self.assertEqual(extension["detailStatus"], "FAILED")
        self.assertIsNone(extension["detailCollectedAt"])
        self.assertIsNone(
            map_boss_results(
                list_root(),
                [detail(overrides={"detail_collected_at": "2026-07-22T23:14:10"})],
            )[0]["extension"]["detailCollectedAt"]
        )

    def test_required_identity_title_and_collection_time_fail_explicitly(self):
        cases = (
            ({"encrypt_job_id": ""}, None, "encrypt_job_id"),
            ({"title": ""}, None, "title"),
            ({}, "", "scraped_at"),
        )
        for job_overrides, scraped_at, expected in cases:
            with self.subTest(expected=expected):
                root = list_root(job_overrides=job_overrides)
                if scraped_at is not None:
                    root["scraped_at"] = scraped_at
                with self.assertRaisesRegex(BossMappingError, expected):
                    map_boss_results(root, [])


def list_root(job_overrides=None, scraped_at="2026-07-22T23:14:09.656461+08:00"):
    job = {
        "job_id": "local-1",
        "title": "Java开发工程师",
        "salary": "20-35K·13薪",
        "salary_source": "api",
        "location": "成都·武侯区·中和",
        "tags": "3-5年 | 本科 | 自定义标签",
        "boss_name": "示例公司",
        "boss_title": "招聘经理",
        "boss_online": False,
        "boss_online_observed_at": None,
        "company_scale": "100-499人",
        "company_stage": "B轮",
        "company_industry": "互联网",
        "job_labels": "3-5年 | 本科",
        "skills": "Java | Spring Boot | 不接受居家办公",
        "security_id": "removed",
        "lid": "removed",
        "encrypt_job_id": "source-1",
        "encrypt_boss_id": "boss-1",
        "encrypt_brand_id": "brand-1",
        "job_link": "https://example.test/job/source-1",
        "company_link": "https://example.test/company/brand-1",
        "welfare": "五险一金 | 带薪年假 | 五险一金",
    }
    if job_overrides:
        job.update(job_overrides)
    return {
        "keyword": "java",
        "city": "成都",
        "filters": {"salary": "20-50K"},
        "filter_desc": ["薪资 20-50K"],
        "scraped_at": scraped_at,
        "total": 1,
        "jobs": [job],
    }


def detail(overrides=None):
    value = {
        "job_id": "local-1",
        "title": "Java开发工程师",
        "company": "示例公司",
        "salary": "20-35K·13薪",
        "location": "成都·武侯区·中和",
        "tags_list": "3-5年 | 本科 | 自定义标签",
        "job_link": "https://example.test/job/source-1",
        "detail_status": "FETCHED",
        "detail_collected_at": "2026-07-22T23:14:10+08:00",
        "skill_tags": [],
        "jd": "完整 JD",
    }
    if overrides:
        value.update(overrides)
    return value


if __name__ == "__main__":
    unittest.main()
