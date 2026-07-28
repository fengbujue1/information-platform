import importlib.util
import io
import json
import pathlib
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from unittest import mock

from integrations import (
    FileOutbox,
    HubClientConfig,
    submit_boss_results_to_hub,
)

ROOT_PATH = pathlib.Path(__file__).resolve().parents[1]
SCRIPT_PATH = ROOT_PATH / "scripts" / "boss_cdp_raw.py"


def load_module():
    sys.modules.setdefault("websocket", mock.Mock())
    sys.modules.setdefault("requests", mock.Mock())
    spec = importlib.util.spec_from_file_location("boss_cdp_raw_raw_capture", SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class RawResponseCaptureTests(unittest.TestCase):
    def test_search_response_parser_preserves_raw_body_and_jobs(self):
        module = load_module()
        raw_body = '{"code":0,"zpData":{"jobList":[{"jobName":"Java"}]}}'
        evaluated = json.dumps(
            {
                "http_status": 200,
                "response_text": raw_body,
                "jobs": [
                    {
                        "title": "Java",
                        "job_link": "https://www.zhipin.com/job_detail/id.html",
                    }
                ],
            },
            ensure_ascii=False,
        )

        response = module.parse_api_search_response(evaluated)

        self.assertEqual(response.http_status, 200)
        self.assertEqual(response.response_text, raw_body)
        self.assertEqual(response.jobs[0]["title"], "Java")
        self.assertEqual(
            module.parse_api_jobs_eval_value(evaluated),
            response.jobs,
        )

    def test_search_response_parser_remains_compatible_with_legacy_job_array(self):
        module = load_module()
        legacy = json.dumps(
            [{"title": "Java", "job_link": "https://example.com/job"}]
        )

        response = module.parse_api_search_response(legacy)

        self.assertEqual(response.http_status, 200)
        self.assertEqual(response.response_text, "")
        self.assertEqual(response.jobs[0]["title"], "Java")

    def test_non_200_response_never_produces_jobs(self):
        module = load_module()
        evaluated = json.dumps(
            {
                "http_status": 403,
                "response_text": "访问受限",
                "jobs": [{"title": "must-not-be-used"}],
            },
            ensure_ascii=False,
        )

        response = module.parse_api_search_response(evaluated)

        self.assertEqual(response.http_status, 403)
        self.assertEqual(response.response_text, "访问受限")
        self.assertEqual(response.jobs, [])

    def test_capture_disabled_does_not_print_or_create_directory(self):
        module = load_module()
        response = module.ApiSearchResponse(200, '{"code":0}', [])

        with tempfile.TemporaryDirectory() as tmp:
            output_dir = pathlib.Path(tmp) / "raw-responses"
            stdout = io.StringIO()
            with redirect_stdout(stdout):
                path = module.maybe_capture_raw_search_response(
                    False,
                    response,
                    str(output_dir),
                    None,
                    1,
                    1,
                )

            self.assertIsNone(path)
            self.assertEqual(stdout.getvalue(), "")
            self.assertFalse(output_dir.exists())

    def test_json_response_is_printed_and_saved_without_reserialization(self):
        module = load_module()
        raw_body = '{\n  "code": 0,\n  "city": "成都"\n}'
        response = module.ApiSearchResponse(200, raw_body, [])

        with tempfile.TemporaryDirectory() as tmp:
            stdout = io.StringIO()
            with redirect_stdout(stdout):
                saved = module.maybe_capture_raw_search_response(
                    True,
                    response,
                    tmp,
                    "run-a",
                    1,
                    1,
                )

            saved_path = pathlib.Path(saved)
            self.assertEqual(saved_path.suffix, ".json")
            self.assertEqual(saved_path.read_text(encoding="utf-8"), raw_body)
            self.assertIn(raw_body, stdout.getvalue())
            self.assertIn("page=1 status=200", stdout.getvalue())
            self.assertNotIn("Cookie", saved_path.name)
            self.assertNotIn("Authorization", saved_path.name)

    def test_non_json_error_body_is_saved_as_text_with_status(self):
        module = load_module()
        raw_body = "<html>访问受限</html>"
        response = module.ApiSearchResponse(403, raw_body, [])

        with tempfile.TemporaryDirectory() as tmp:
            saved = module.maybe_capture_raw_search_response(
                True,
                response,
                tmp,
                "run-b",
                2,
                2,
            )

            saved_path = pathlib.Path(saved)
            self.assertEqual(saved_path.suffix, ".txt")
            self.assertIn("page_002", saved_path.name)
            self.assertIn("request_002", saved_path.name)
            self.assertIn("status_403", saved_path.name)
            self.assertEqual(saved_path.read_text(encoding="utf-8"), raw_body)

    def test_multiple_requests_create_unique_files_without_overwrite(self):
        module = load_module()
        response = module.ApiSearchResponse(200, '{"code":0}', [])

        with tempfile.TemporaryDirectory() as tmp:
            first = module.maybe_capture_raw_search_response(
                True, response, tmp, "same-run", 1, 1
            )
            second = module.maybe_capture_raw_search_response(
                True, response, tmp, "same-run", 2, 2
            )

            self.assertNotEqual(first, second)
            self.assertEqual(len(list(pathlib.Path(tmp).iterdir())), 2)

    def test_capture_write_failure_raises_explicit_error(self):
        module = load_module()
        response = module.ApiSearchResponse(200, '{"code":0}', [])

        with mock.patch.object(
            module.os,
            "makedirs",
            side_effect=PermissionError("denied"),
        ):
            with self.assertRaisesRegex(
                module.RawResponseCaptureError,
                "无法保存 BOSS 原始响应",
            ):
                module.maybe_capture_raw_search_response(
                    True,
                    response,
                    "blocked",
                    "run-c",
                    1,
                    1,
                )

    def test_xhr_template_returns_raw_body_before_field_mapping(self):
        module = load_module()
        template = module.FETCH_API_JS_TEMPLATE

        self.assertIn("response_text: responseText", template)
        self.assertIn("http_status: xhr.status", template)
        self.assertLess(template.index("response_text"), template.index("j.jobName"))

    def test_cli_help_documents_raw_response_options(self):
        module = load_module()
        stdout = io.StringIO()

        with mock.patch.object(
            sys,
            "argv",
            ["boss_cdp_raw.py", "--help"],
        ), redirect_stdout(stdout), self.assertRaises(SystemExit) as exit_result:
            module.main()

        self.assertEqual(exit_result.exception.code, 0)
        self.assertIn("--capture-raw-response", stdout.getvalue())
        self.assertIn("--raw-response-dir", stdout.getvalue())

    def test_default_raw_directory_is_covered_by_gitignore(self):
        module = load_module()
        gitignore = (ROOT_PATH / ".gitignore").read_text(encoding="utf-8")

        self.assertEqual(
            pathlib.Path(module.DEFAULT_RAW_RESPONSE_DIR).parent,
            pathlib.Path(module.DEFAULT_RESULT_DIR),
        )
        self.assertIn("result/job-result/", gitignore)

    def test_fresh_scrape_can_queue_retryable_hub_failure(self):
        module = load_module()
        job = {
            "title": "Java Engineer",
            "salary": "20-30K",
            "location": "上海·浦东新区·张江",
            "tags": "3-5年 | 本科",
            "boss_name": "Example Company",
            "boss_title": "招聘者",
            "boss_online": False,
            "encrypt_job_id": "encrypted-job-1",
            "encrypt_boss_id": "encrypted-boss-1",
            "encrypt_brand_id": "encrypted-brand-1",
            "job_link": (
                "https://www.zhipin.com/job_detail/"
                "encrypted-job-1.html"
            ),
        }
        cdp = mock.Mock()

        with tempfile.TemporaryDirectory() as tmp, mock.patch.object(
            module,
            "resolve_city",
            return_value=("上海", "101020100"),
        ), mock.patch.object(
            module,
            "CDPSession",
            return_value=cdp,
        ), mock.patch.object(
            module,
            "create_page_session",
            return_value=("target-1", "session-1"),
        ), mock.patch.object(
            module,
            "parse_api_search_response",
            return_value=module.ApiSearchResponse(200, "{}", [job]),
        ), mock.patch.object(
            module.time,
            "sleep",
        ), mock.patch.object(
            module,
            "incr_request",
        ), redirect_stdout(io.StringIO()):
            output_path = pathlib.Path(tmp) / "boss_jobs.json"
            list_result = module.scrape_list(
                "Java",
                "上海",
                1,
                {},
                str(output_path),
            )

            session = mock.Mock()
            response = mock.Mock()
            response.status_code = 503
            session.post.return_value = response
            outbox = FileOutbox(pathlib.Path(tmp) / "outbox")
            submit_result = submit_boss_results_to_hub(
                list_result,
                [],
                client_config=HubClientConfig(
                    enabled=True,
                    url=(
                        "https://hub.example.test/"
                        "api/v1/collector/items"
                    ),
                    token="test-token",
                ),
                session=session,
                outbox=outbox,
            )

            saved_result = json.loads(
                output_path.read_text(encoding="utf-8")
            )
            pending_files = list(outbox.pending_dir.glob("*.json"))

        self.assertTrue(list_result["scraped_at"])
        self.assertEqual(
            list_result["scraped_at"],
            saved_result["scraped_at"],
        )
        self.assertTrue(submit_result.mapping_succeeded)
        self.assertEqual(submit_result.queued, 1)
        self.assertEqual(len(pending_files), 1)


if __name__ == "__main__":
    unittest.main()
