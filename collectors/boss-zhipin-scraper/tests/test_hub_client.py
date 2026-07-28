import logging
import pathlib
import tempfile
import unittest
from unittest import mock

import requests

import integrations.config as config_module
from integrations import (
    HubClientConfig,
    HubSubmitOutcome,
    InformationHubClient,
    MapperConfig,
    submit_boss_results_to_hub,
)


class HubClientConfigTests(unittest.TestCase):
    def test_hub_is_disabled_by_default_without_other_configuration(self):
        config = HubClientConfig.from_environment({})

        self.assertFalse(config.enabled)
        self.assertEqual(config.url, "")
        self.assertEqual(config.token, "")

    def test_enabled_configuration_reads_url_token_and_timeouts(self):
        token = "secret-token-for-test"

        config = HubClientConfig.from_environment(
            {
                "INFORMATION_HUB_ENABLED": "true",
                "INFORMATION_HUB_URL": (
                    "http://127.0.0.1:8080/api/v1/collector/items"
                ),
                "INFORMATION_HUB_COLLECTOR_TOKEN": token,
                "INFORMATION_HUB_CONNECT_TIMEOUT_SECONDS": "1.5",
                "INFORMATION_HUB_READ_TIMEOUT_SECONDS": "4",
            }
        )

        self.assertTrue(config.enabled)
        self.assertEqual(
            config.url,
            "http://127.0.0.1:8080/api/v1/collector/items",
        )
        self.assertEqual(config.token, token)
        self.assertEqual(config.connect_timeout_seconds, 1.5)
        self.assertEqual(config.read_timeout_seconds, 4.0)
        self.assertNotIn(token, repr(config))

    def test_ini_file_configures_hub_and_mapper_without_environment(self):
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / "collector.ini"
            path.write_text(
                """
[collector]
collector_id = local-collector
collector_version = 9.1.0
historical_timezone = UTC

[information_hub]
enabled = true
url = http://127.0.0.1:8080/api/v1/collector/items
collector_token = ini-token
connect_timeout_seconds = 2.5
read_timeout_seconds = 8
""".strip(),
                encoding="utf-8",
            )

            hub_config = HubClientConfig.from_sources(path, {})
            mapper_config = MapperConfig.from_config_file(path)

        self.assertTrue(hub_config.enabled)
        self.assertEqual(
            hub_config.url,
            "http://127.0.0.1:8080/api/v1/collector/items",
        )
        self.assertEqual(hub_config.token, "ini-token")
        self.assertEqual(hub_config.connect_timeout_seconds, 2.5)
        self.assertEqual(hub_config.read_timeout_seconds, 8.0)
        self.assertEqual(mapper_config.collector_id, "local-collector")
        self.assertEqual(mapper_config.collector_version, "9.1.0")
        self.assertEqual(mapper_config.historical_timezone, "UTC")

    def test_environment_values_override_ini_file(self):
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / "collector.ini"
            path.write_text(
                """
[information_hub]
enabled = true
url = http://ini.example.test/items
collector_token = ini-token
connect_timeout_seconds = 2
read_timeout_seconds = 8
""".strip(),
                encoding="utf-8",
            )

            config = HubClientConfig.from_sources(
                path,
                {
                    "INFORMATION_HUB_URL": (
                        "https://environment.example.test/items"
                    ),
                    "INFORMATION_HUB_COLLECTOR_TOKEN": "environment-token",
                    "INFORMATION_HUB_CONNECT_TIMEOUT_SECONDS": "4",
                    "INFORMATION_HUB_READ_TIMEOUT_SECONDS": "12",
                },
            )

        self.assertTrue(config.enabled)
        self.assertEqual(
            config.url,
            "https://environment.example.test/items",
        )
        self.assertEqual(config.token, "environment-token")
        self.assertEqual(config.connect_timeout_seconds, 4.0)
        self.assertEqual(config.read_timeout_seconds, 12.0)

    def test_missing_default_file_keeps_hub_disabled(self):
        with tempfile.TemporaryDirectory() as directory:
            missing = pathlib.Path(directory) / "collector.ini"
            with mock.patch.object(config_module, "DEFAULT_CONFIG_PATH", missing):
                config = HubClientConfig.from_sources(environ={})

        self.assertFalse(config.enabled)

    def test_missing_explicit_or_malformed_file_fails_safely(self):
        with tempfile.TemporaryDirectory() as directory:
            missing = pathlib.Path(directory) / "missing.ini"
            malformed = pathlib.Path(directory) / "malformed.ini"
            malformed.write_text("[information_hub", encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "does not exist"):
                HubClientConfig.from_sources(missing, {})
            with self.assertRaisesRegex(ValueError, "could not be read"):
                HubClientConfig.from_sources(malformed, {})

    def test_invalid_environment_values_fail_without_echoing_values(self):
        with self.assertRaisesRegex(ValueError, "must be a boolean"):
            HubClientConfig.from_environment(
                {"INFORMATION_HUB_ENABLED": "maybe"}
            )
        with self.assertRaisesRegex(ValueError, "positive number"):
            HubClientConfig.from_environment(
                {
                    "INFORMATION_HUB_ENABLED": "true",
                    "INFORMATION_HUB_CONNECT_TIMEOUT_SECONDS": "0",
                }
            )

    def test_enabled_configuration_requires_http_url_and_token(self):
        missing_url = HubClientConfig(enabled=True, token="hidden")
        invalid_url = HubClientConfig(
            enabled=True,
            url="file:///tmp/items",
            token="hidden",
        )
        missing_token = HubClientConfig(
            enabled=True,
            url="https://hub.example.test/api/v1/collector/items",
        )

        self.assertIn("INFORMATION_HUB_URL", missing_url.validation_error())
        self.assertIn("http or https", invalid_url.validation_error())
        self.assertIn(
            "INFORMATION_HUB_COLLECTOR_TOKEN",
            missing_token.validation_error(),
        )


class InformationHubClientTests(unittest.TestCase):
    def setUp(self):
        self.token = "never-log-this-token"
        self.config = HubClientConfig(
            enabled=True,
            url="https://hub.example.test/api/v1/collector/items",
            token=self.token,
            connect_timeout_seconds=2.0,
            read_timeout_seconds=7.0,
        )
        self.session = mock.Mock()
        self.logger = logging.getLogger(
            f"{__name__}.{self._testMethodName}"
        )
        self.client = InformationHubClient(
            self.config,
            session=self.session,
            logger=self.logger,
        )
        self.envelope = {
            "sourceItemId": "source-1",
            "rawPayload": {"safe": "value"},
        }

    def test_disabled_client_does_not_send_request(self):
        client = InformationHubClient(
            HubClientConfig(enabled=False),
            session=self.session,
            logger=self.logger,
        )

        result = client.submit(self.envelope)

        self.assertEqual(result.outcome, HubSubmitOutcome.DISABLED)
        self.session.post.assert_not_called()

    def test_invalid_configuration_does_not_send_request(self):
        client = InformationHubClient(
            HubClientConfig(enabled=True, token=self.token),
            session=self.session,
            logger=self.logger,
        )

        with self.assertLogs(self.logger.name, level="WARNING") as captured:
            result = client.submit(self.envelope)

        self.assertEqual(
            result.outcome,
            HubSubmitOutcome.CONFIGURATION_ERROR,
        )
        self.session.post.assert_not_called()
        self.assertNotIn(self.token, "\n".join(captured.output))

    def test_every_2xx_status_is_successful_and_request_is_authenticated(self):
        for status_code in (200, 201, 202, 204, 299):
            with self.subTest(status_code=status_code):
                self.session.reset_mock()
                self.session.post.return_value = response(status_code)

                result = self.client.submit(self.envelope)

                self.assertTrue(result.succeeded)
                self.assertEqual(result.status_code, status_code)
                self.session.post.assert_called_once_with(
                    self.config.url,
                    json=self.envelope,
                    headers={
                        "Authorization": f"Bearer {self.token}",
                        "Accept": "application/json",
                    },
                    timeout=(2.0, 7.0),
                )

    def test_4xx_and_413_are_classified_without_stopping_batch(self):
        for status_code, outcome in (
            (400, HubSubmitOutcome.CLIENT_ERROR),
            (401, HubSubmitOutcome.CLIENT_ERROR),
            (403, HubSubmitOutcome.CLIENT_ERROR),
            (404, HubSubmitOutcome.CLIENT_ERROR),
            (413, HubSubmitOutcome.PAYLOAD_TOO_LARGE),
            (422, HubSubmitOutcome.CLIENT_ERROR),
        ):
            with self.subTest(status_code=status_code):
                self.session.post.return_value = response(status_code)

                result = self.client.submit(self.envelope)

                self.assertEqual(result.outcome, outcome)
                self.assertFalse(result.should_stop_batch)

    def test_5xx_and_unexpected_http_status_stop_batch(self):
        for status_code, outcome in (
            (500, HubSubmitOutcome.SERVER_ERROR),
            (503, HubSubmitOutcome.SERVER_ERROR),
            (302, HubSubmitOutcome.HTTP_ERROR),
        ):
            with self.subTest(status_code=status_code):
                self.session.post.return_value = response(status_code)

                result = self.client.submit(self.envelope)

                self.assertEqual(result.outcome, outcome)
                self.assertTrue(result.should_stop_batch)

    def test_timeout_connection_and_request_failures_are_safe(self):
        cases = (
            (requests.ConnectTimeout("sensitive"), HubSubmitOutcome.TIMEOUT),
            (requests.ReadTimeout("sensitive"), HubSubmitOutcome.TIMEOUT),
            (
                requests.ConnectionError("sensitive"),
                HubSubmitOutcome.CONNECTION_FAILED,
            ),
            (
                requests.RequestException("sensitive"),
                HubSubmitOutcome.REQUEST_FAILED,
            ),
        )
        for exception, outcome in cases:
            with self.subTest(outcome=outcome):
                self.session.post.side_effect = exception

                result = self.client.submit(self.envelope)

                self.assertEqual(result.outcome, outcome)
                self.assertTrue(result.should_stop_batch)

    def test_logs_never_contain_token_payload_or_response_body(self):
        self.session.post.return_value = response(
            401,
            text=f"{self.token} raw-payload-secret",
        )
        envelope = {
            "sourceItemId": "source-1",
            "rawPayload": {"value": "raw-payload-secret"},
        }

        with self.assertLogs(self.logger.name, level="WARNING") as captured:
            self.client.submit(envelope)

        output = "\n".join(captured.output)
        self.assertNotIn(self.token, output)
        self.assertNotIn("raw-payload-secret", output)
        self.assertNotIn("Authorization", output)


class HubBatchSubmissionTests(unittest.TestCase):
    def setUp(self):
        self.logger = logging.getLogger(
            f"{__name__}.{self._testMethodName}"
        )
        self.config = HubClientConfig(
            enabled=True,
            url="https://hub.example.test/api/v1/collector/items",
            token="batch-secret",
        )
        self.mapper_config = MapperConfig(
            run_id_factory=lambda: "hub-test-run"
        )

    def test_disabled_batch_does_not_map_or_send(self):
        session = mock.Mock()

        result = submit_boss_results_to_hub(
            None,
            client_config=HubClientConfig(enabled=False),
            session=session,
            logger=self.logger,
        )

        self.assertFalse(result.enabled)
        self.assertEqual(result.total, 0)
        session.post.assert_not_called()

    def test_client_errors_continue_with_later_items(self):
        session = mock.Mock()
        session.post.side_effect = [response(400), response(413), response(201)]

        result = submit_boss_results_to_hub(
            list_root(job_count=3),
            [],
            client_config=self.config,
            mapper_config=self.mapper_config,
            session=session,
            logger=self.logger,
        )

        self.assertEqual(result.total, 3)
        self.assertEqual(result.succeeded, 1)
        self.assertEqual(result.failed, 2)
        self.assertEqual(result.skipped, 0)
        self.assertEqual(session.post.call_count, 3)

    def test_server_error_stops_remaining_items(self):
        session = mock.Mock()
        session.post.return_value = response(503)
        outbox = RecordingOutbox()

        result = submit_boss_results_to_hub(
            list_root(job_count=3),
            [],
            client_config=self.config,
            mapper_config=self.mapper_config,
            session=session,
            outbox=outbox,
            logger=self.logger,
        )

        self.assertEqual(result.total, 3)
        self.assertEqual(result.succeeded, 0)
        self.assertEqual(result.failed, 1)
        self.assertEqual(result.skipped, 2)
        self.assertEqual(result.queued, 3)
        self.assertEqual(len(outbox.entries), 3)
        self.assertEqual(outbox.entries[0][1], "SERVER_ERROR")
        session.post.assert_called_once()

    def test_timeout_and_connection_failure_stop_remaining_items(self):
        for exception in (
            requests.Timeout("timeout"),
            requests.ConnectionError("connection"),
        ):
            with self.subTest(exception=type(exception).__name__):
                session = mock.Mock()
                session.post.side_effect = exception
                outbox = RecordingOutbox()

                result = submit_boss_results_to_hub(
                    list_root(job_count=3),
                    [],
                    client_config=self.config,
                    mapper_config=self.mapper_config,
                    session=session,
                    outbox=outbox,
                    logger=self.logger,
                )

                self.assertEqual(result.failed, 1)
                self.assertEqual(result.skipped, 2)
                self.assertEqual(result.queued, 3)
                self.assertEqual(len(outbox.entries), 3)
                session.post.assert_called_once()

    def test_invalid_configuration_and_mapping_failure_do_not_propagate(self):
        session = mock.Mock()

        invalid_config = submit_boss_results_to_hub(
            list_root(),
            [],
            client_config=HubClientConfig(enabled=True),
            session=session,
            logger=self.logger,
        )
        mapping_failure = submit_boss_results_to_hub(
            {"jobs": []},
            [],
            client_config=self.config,
            session=session,
            logger=self.logger,
        )

        self.assertFalse(invalid_config.configuration_valid)
        self.assertFalse(mapping_failure.mapping_succeeded)
        session.post.assert_not_called()

    def test_explicit_missing_config_file_does_not_propagate(self):
        session = mock.Mock()
        missing = pathlib.Path("definitely-missing-collector-config.ini")

        result = submit_boss_results_to_hub(
            list_root(),
            [],
            config_path=missing,
            environ={},
            session=session,
            logger=self.logger,
        )

        self.assertTrue(result.enabled)
        self.assertFalse(result.configuration_valid)
        session.post.assert_not_called()

    def test_batch_uses_ini_config_for_mapper_and_http_client(self):
        session = mock.Mock()
        session.post.return_value = response(201)
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / "collector.ini"
            path.write_text(
                """
[collector]
collector_id = ini-collector
collector_version = 4.0.0
historical_timezone = Asia/Shanghai

[information_hub]
enabled = true
url = https://hub.example.test/items
collector_token = ini-batch-token
connect_timeout_seconds = 1
read_timeout_seconds = 5
""".strip(),
                encoding="utf-8",
            )

            result = submit_boss_results_to_hub(
                list_root(),
                [],
                config_path=path,
                environ={},
                session=session,
                logger=self.logger,
            )

        self.assertEqual(result.succeeded, 1)
        request = session.post.call_args
        self.assertEqual(
            request.args[0],
            "https://hub.example.test/items",
        )
        self.assertEqual(
            request.kwargs["json"]["collector"],
            {
                "collectorId": "ini-collector",
                "collectorVersion": "4.0.0",
            },
        )
        self.assertEqual(
            request.kwargs["headers"]["Authorization"],
            "Bearer ini-batch-token",
        )
        self.assertEqual(request.kwargs["timeout"], (1.0, 5.0))

    def test_unexpected_client_failure_does_not_propagate(self):
        session = mock.Mock()
        session.post.side_effect = RuntimeError(
            "batch-secret raw-payload-secret"
        )
        outbox = RecordingOutbox()

        with self.assertLogs(self.logger.name, level="WARNING") as captured:
            result = submit_boss_results_to_hub(
                list_root(job_count=2),
                [],
                client_config=self.config,
                mapper_config=self.mapper_config,
                session=session,
                outbox=outbox,
                logger=self.logger,
            )

        self.assertEqual(result.failed, 1)
        self.assertEqual(result.skipped, 1)
        self.assertEqual(result.queued, 2)
        self.assertEqual(len(outbox.entries), 2)
        output = "\n".join(captured.output)
        self.assertNotIn("batch-secret", output)
        self.assertNotIn("raw-payload-secret", output)

    def test_outbox_write_failure_does_not_propagate_or_leak_payload(self):
        session = mock.Mock()
        session.post.return_value = response(503)

        with self.assertLogs(self.logger.name, level="WARNING") as captured:
            result = submit_boss_results_to_hub(
                list_root(job_count=2),
                [],
                client_config=self.config,
                mapper_config=self.mapper_config,
                session=session,
                outbox=FailingOutbox(),
                logger=self.logger,
            )

        self.assertEqual(result.failed, 1)
        self.assertEqual(result.skipped, 1)
        self.assertEqual(result.queued, 0)
        output = "\n".join(captured.output)
        self.assertNotIn("batch-secret", output)
        self.assertNotIn("rawPayload", output)


def response(status_code, text=""):
    value = mock.Mock()
    value.status_code = status_code
    value.text = text
    return value


def list_root(job_count=1):
    jobs = []
    for index in range(job_count):
        jobs.append(
            {
                "job_id": f"local-{index}",
                "encrypt_job_id": f"source-{index}",
                "title": f"Job {index}",
                "salary": "20-30K",
                "location": "成都·高新区",
                "tags": "3-5年 | 本科",
                "skills": "Python | MySQL",
                "welfare": "五险一金",
                "boss_online": False,
            }
        )
    return {
        "keyword": "Python",
        "city": "成都",
        "filters": {},
        "filter_desc": [],
        "scraped_at": "2026-07-27T09:00:00+08:00",
        "total": job_count,
        "jobs": jobs,
    }


class RecordingOutbox:
    def __init__(self):
        self.entries = []

    def enqueue(self, envelope, *, last_outcome):
        self.entries.append((envelope, last_outcome))


class FailingOutbox:
    def enqueue(self, _envelope, *, last_outcome):
        raise OSError(f"cannot persist {last_outcome}")



if __name__ == "__main__":
    unittest.main()
