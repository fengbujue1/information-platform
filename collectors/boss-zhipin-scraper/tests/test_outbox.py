import json
import os
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from unittest import mock

from integrations.hub_client import (
    HubClientConfig,
    HubSubmitOutcome,
    HubSubmitResult,
    flush_information_hub_outbox,
)
from integrations.outbox import FileOutbox


class MutableClock:
    def __init__(self, value):
        self.value = value

    def __call__(self):
        return self.value


class FileOutboxTests(unittest.TestCase):
    def setUp(self):
        self.now = datetime(2026, 7, 28, 8, 0, tzinfo=timezone.utc)
        self.clock = MutableClock(self.now)

    def test_enqueue_uses_same_directory_atomic_replace(self):
        with tempfile.TemporaryDirectory() as directory:
            replacements = []

            def replace(source, destination):
                replacements.append((Path(source), Path(destination)))
                os.replace(source, destination)

            outbox = FileOutbox(
                directory,
                clock=self.clock,
                replace=replace,
            )

            path = outbox.enqueue(
                envelope("source-1"),
                last_outcome=HubSubmitOutcome.TIMEOUT.value,
            )

            self.assertTrue(path.exists())
            self.assertEqual(len(replacements), 1)
            source, destination = replacements[0]
            self.assertEqual(source.parent, destination.parent)
            self.assertEqual(destination, path)
            self.assertEqual(list(path.parent.glob("*.tmp")), [])
            entry = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual(entry["retryCount"], 0)
            self.assertEqual(entry["nextRetryAt"], "2026-07-28T08:00:00.000Z")

    def test_failed_atomic_replace_leaves_no_visible_or_temporary_file(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = FileOutbox(
                directory,
                clock=self.clock,
                replace=lambda _source, _destination: (_ for _ in ()).throw(
                    OSError("replace failed")
                ),
            )

            with self.assertRaises(OSError):
                outbox.enqueue(
                    envelope("source-1"),
                    last_outcome=HubSubmitOutcome.CONNECTION_FAILED.value,
                )

            self.assertEqual(
                [
                    path
                    for path in Path(directory).rglob("*")
                    if path.is_file()
                ],
                [],
            )

    def test_successful_flush_removes_pending_file(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = FileOutbox(directory, clock=self.clock)
            outbox.enqueue(
                envelope("source-1"),
                last_outcome=HubSubmitOutcome.SERVER_ERROR.value,
            )

            result = outbox.flush(
                lambda _envelope: HubSubmitResult(
                    HubSubmitOutcome.SUCCESS,
                    201,
                )
            )

            self.assertEqual(result.succeeded, 1)
            self.assertEqual(list(outbox.pending_dir.glob("*.json")), [])

    def test_retryable_failure_increments_count_and_delays_next_attempt(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = FileOutbox(
                directory,
                clock=self.clock,
                initial_retry_seconds=60,
                max_retry_seconds=3600,
            )
            outbox.enqueue(
                envelope("source-1"),
                last_outcome=HubSubmitOutcome.TIMEOUT.value,
            )

            failed = outbox.flush(
                lambda _envelope: HubSubmitResult(
                    HubSubmitOutcome.SERVER_ERROR,
                    503,
                )
            )

            self.assertEqual(failed.rescheduled, 1)
            self.assertTrue(failed.stopped)
            path = next(outbox.pending_dir.glob("*.json"))
            entry = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual(entry["retryCount"], 1)
            self.assertEqual(
                entry["nextRetryAt"],
                "2026-07-28T08:01:00.000Z",
            )

            submit_calls = []
            skipped = outbox.flush(
                lambda item: submit_calls.append(item)
            )
            self.assertEqual(skipped.skipped, 1)
            self.assertEqual(submit_calls, [])

            self.clock.value = self.now + timedelta(seconds=60)
            succeeded = outbox.flush(
                lambda _envelope: HubSubmitResult(
                    HubSubmitOutcome.SUCCESS,
                    200,
                )
            )
            self.assertEqual(succeeded.succeeded, 1)

    def test_large_retry_count_uses_capped_delay_without_large_exponent(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = FileOutbox(
                directory,
                clock=self.clock,
                initial_retry_seconds=60,
                max_retry_seconds=3600,
            )
            path = outbox.enqueue(
                envelope("source-1"),
                last_outcome=HubSubmitOutcome.TIMEOUT.value,
            )
            entry = json.loads(path.read_text(encoding="utf-8"))
            entry["retryCount"] = 1_000_000
            path.write_text(json.dumps(entry), encoding="utf-8")

            result = outbox.flush(
                lambda _envelope: HubSubmitResult(
                    HubSubmitOutcome.SERVER_ERROR,
                    503,
                )
            )

            self.assertEqual(result.rescheduled, 1)
            updated = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual(updated["retryCount"], 1_000_001)
            self.assertEqual(
                updated["nextRetryAt"],
                "2026-07-28T09:00:00.000Z",
            )

    def test_corrupt_file_is_quarantined_without_blocking_valid_entry(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = FileOutbox(directory, clock=self.clock)
            outbox.pending_dir.mkdir(parents=True)
            corrupt = outbox.pending_dir / "000-corrupt.json"
            corrupt.write_text("{not-json", encoding="utf-8")
            outbox.enqueue(
                envelope("source-1"),
                last_outcome=HubSubmitOutcome.TIMEOUT.value,
            )

            result = outbox.flush(
                lambda _envelope: HubSubmitResult(
                    HubSubmitOutcome.SUCCESS,
                    201,
                )
            )

            self.assertEqual(result.quarantined, 1)
            self.assertEqual(result.succeeded, 1)
            self.assertTrue(
                (outbox.quarantine_dir / corrupt.name).exists()
            )

    def test_quarantine_failure_does_not_block_later_valid_entry(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = FileOutbox(directory, clock=self.clock)
            outbox.pending_dir.mkdir(parents=True)
            corrupt = outbox.pending_dir / "000-corrupt.json"
            corrupt.write_text("{not-json", encoding="utf-8")
            outbox.enqueue(
                envelope("source-1"),
                last_outcome=HubSubmitOutcome.TIMEOUT.value,
            )

            original_replace = outbox._replace

            def fail_only_for_corrupt(source, destination):
                if Path(source).name == corrupt.name:
                    raise OSError("quarantine failed")
                original_replace(source, destination)

            outbox._replace = fail_only_for_corrupt
            result = outbox.flush(
                lambda _envelope: HubSubmitResult(
                    HubSubmitOutcome.SUCCESS,
                    201,
                )
            )

            self.assertEqual(result.quarantined, 0)
            self.assertEqual(result.succeeded, 1)
            self.assertTrue(corrupt.exists())

    def test_non_retryable_response_moves_entry_to_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = FileOutbox(directory, clock=self.clock)
            outbox.enqueue(
                envelope("source-1"),
                last_outcome=HubSubmitOutcome.TIMEOUT.value,
            )

            result = outbox.flush(
                lambda _envelope: HubSubmitResult(
                    HubSubmitOutcome.CLIENT_ERROR,
                    400,
                )
            )

            self.assertEqual(result.rejected, 1)
            self.assertEqual(list(outbox.pending_dir.glob("*.json")), [])
            rejected = next(outbox.rejected_dir.glob("*.json"))
            entry = json.loads(rejected.read_text(encoding="utf-8"))
            self.assertEqual(entry["lastOutcome"], "CLIENT_ERROR")
            self.assertIn("rejectedAt", entry)

    def test_duplicate_business_key_is_resent_without_mutating_envelope(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = FileOutbox(directory, clock=self.clock)
            original = envelope("same-source-item")
            outbox.enqueue(
                original,
                last_outcome=HubSubmitOutcome.TIMEOUT.value,
            )
            outbox.enqueue(
                original,
                last_outcome=HubSubmitOutcome.TIMEOUT.value,
            )
            submitted = []

            result = outbox.flush(
                lambda item: (
                    submitted.append(item)
                    or HubSubmitResult(HubSubmitOutcome.SUCCESS, 200)
                )
            )

            self.assertEqual(result.succeeded, 2)
            self.assertEqual(len(submitted), 2)
            self.assertEqual(submitted[0], original)
            self.assertEqual(submitted[1], original)

    def test_hub_flush_submits_pending_entry_without_external_services(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = FileOutbox(directory, clock=self.clock)
            outbox.enqueue(
                envelope("source-1"),
                last_outcome=HubSubmitOutcome.CONNECTION_FAILED.value,
            )
            session = mock.Mock()
            response = mock.Mock()
            response.status_code = 201
            session.post.return_value = response
            config = HubClientConfig(
                enabled=True,
                url="https://hub.example.test/api/v1/collector/items",
                token="test-token",
            )

            result = flush_information_hub_outbox(
                client_config=config,
                session=session,
                outbox=outbox,
            )

            self.assertTrue(result.succeeded)
            self.assertEqual(result.outbox.succeeded, 1)
            self.assertEqual(list(outbox.pending_dir.glob("*.json")), [])
            session.post.assert_called_once()


def envelope(source_item_id):
    return {
        "schemaVersion": 1,
        "informationType": "JOB",
        "source": "BOSS",
        "sourceItemId": source_item_id,
        "title": "Java Engineer",
        "rawPayload": {"list": {"title": "Java Engineer"}},
    }


if __name__ == "__main__":
    unittest.main()
