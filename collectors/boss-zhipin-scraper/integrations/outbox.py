"""Local file Outbox for sanitized InformationEnvelope payloads."""

import json
import logging
import os
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any, Callable, Mapping
from uuid import uuid4


LOGGER = logging.getLogger(__name__)
OUTBOX_SCHEMA_VERSION = 1
DEFAULT_OUTBOX_DIR = (
    Path(__file__).resolve().parent.parent / "result" / "outbox"
)
DEFAULT_INITIAL_RETRY_SECONDS = 60
DEFAULT_MAX_RETRY_SECONDS = 3600


@dataclass(frozen=True)
class OutboxFlushResult:
    """Summary of one local Outbox flush."""

    total: int = 0
    processed: int = 0
    succeeded: int = 0
    rescheduled: int = 0
    rejected: int = 0
    quarantined: int = 0
    skipped: int = 0
    stopped: bool = False


class FileOutbox:
    """Persist sanitized envelopes and resend due entries safely."""

    def __init__(
        self,
        root: Path | str = DEFAULT_OUTBOX_DIR,
        *,
        clock: Callable[[], datetime] | None = None,
        initial_retry_seconds: int = DEFAULT_INITIAL_RETRY_SECONDS,
        max_retry_seconds: int = DEFAULT_MAX_RETRY_SECONDS,
        replace: Callable[[Path, Path], None] = os.replace,
        logger=None,
    ):
        if initial_retry_seconds <= 0:
            raise ValueError("initial_retry_seconds must be positive")
        if max_retry_seconds < initial_retry_seconds:
            raise ValueError(
                "max_retry_seconds must be greater than or equal to "
                "initial_retry_seconds"
            )
        self.root = Path(root)
        self.pending_dir = self.root / "pending"
        self.quarantine_dir = self.root / "quarantine"
        self.rejected_dir = self.root / "rejected"
        self._clock = clock or (lambda: datetime.now(timezone.utc))
        self._initial_retry_seconds = initial_retry_seconds
        self._max_retry_seconds = max_retry_seconds
        self._replace = replace
        self._logger = logger or LOGGER

    def enqueue(
        self,
        envelope: Mapping[str, Any],
        *,
        last_outcome: str,
    ):
        """Atomically enqueue one already-sanitized InformationEnvelope."""
        now = self._now()
        outbox_id = str(uuid4())
        entry = {
            "schemaVersion": OUTBOX_SCHEMA_VERSION,
            "outboxId": outbox_id,
            "createdAt": _format_utc(now),
            "updatedAt": _format_utc(now),
            "retryCount": 0,
            "nextRetryAt": _format_utc(now),
            "lastOutcome": str(last_outcome),
            "envelope": dict(envelope),
        }
        _validate_entry(entry)
        path = self.pending_dir / (
            f"{now.strftime('%Y%m%dT%H%M%S%fZ')}_{outbox_id}.json"
        )
        self._atomic_write(path, entry)
        return path

    def flush(self, submit: Callable[[Mapping[str, Any]], Any]):
        """Submit due entries without allowing one bad file to block others."""
        now = self._now()
        paths = self._pending_paths()
        processed = 0
        succeeded = 0
        rescheduled = 0
        rejected = 0
        quarantined = 0
        skipped = 0
        stopped = False

        for path in paths:
            try:
                entry = self._read_entry(path)
            except (OSError, UnicodeError, ValueError, json.JSONDecodeError):
                if self._quarantine(path):
                    quarantined += 1
                continue

            if _parse_utc(entry["nextRetryAt"]) > now:
                skipped += 1
                continue

            processed += 1
            try:
                result = submit(entry["envelope"])
            except Exception:
                # 未知客户端异常按可重试失败处理，且不输出异常正文或 payload。
                self._reschedule(path, entry, "REQUEST_FAILED", now)
                rescheduled += 1
                stopped = True
                break

            if bool(getattr(result, "succeeded", False)):
                path.unlink(missing_ok=True)
                succeeded += 1
                continue

            outcome = _outcome_text(getattr(result, "outcome", None))
            if bool(getattr(result, "retryable", False)):
                self._reschedule(path, entry, outcome, now)
                rescheduled += 1
                stopped = True
                break

            self._reject(path, entry, outcome, now)
            rejected += 1

        return OutboxFlushResult(
            total=len(paths),
            processed=processed,
            succeeded=succeeded,
            rescheduled=rescheduled,
            rejected=rejected,
            quarantined=quarantined,
            skipped=skipped,
            stopped=stopped,
        )

    def _pending_paths(self):
        if not self.pending_dir.exists():
            return []
        return sorted(self.pending_dir.glob("*.json"))

    def _read_entry(self, path):
        with path.open(encoding="utf-8") as stream:
            entry = json.load(stream)
        _validate_entry(entry)
        return entry

    def _reschedule(self, path, entry, outcome, now):
        retry_count = entry["retryCount"] + 1
        delay_seconds = self._initial_retry_seconds
        for _ in range(retry_count - 1):
            if delay_seconds >= self._max_retry_seconds:
                break
            delay_seconds = min(
                delay_seconds * 2,
                self._max_retry_seconds,
            )
        updated = dict(entry)
        updated.update(
            {
                "updatedAt": _format_utc(now),
                "retryCount": retry_count,
                "nextRetryAt": _format_utc(
                    now + timedelta(seconds=delay_seconds)
                ),
                "lastOutcome": outcome,
            }
        )
        self._atomic_write(path, updated)

    def _reject(self, path, entry, outcome, now):
        updated = dict(entry)
        updated.update(
            {
                "updatedAt": _format_utc(now),
                "lastOutcome": outcome,
                "rejectedAt": _format_utc(now),
            }
        )
        destination = self._unique_destination(self.rejected_dir, path.name)
        self._atomic_write(destination, updated)
        path.unlink(missing_ok=True)

    def _quarantine(self, path):
        try:
            destination = self._unique_destination(
                self.quarantine_dir,
                path.name,
            )
            self._replace(path, destination)
        except FileNotFoundError:
            return False
        except OSError as exception:
            self._logger.warning(
                "Invalid Outbox file quarantine failed file=%s type=%s",
                path.name,
                type(exception).__name__,
            )
            return False
        self._logger.warning(
            "Invalid Outbox file was quarantined file=%s",
            path.name,
        )
        return True

    def _unique_destination(self, directory, filename):
        directory.mkdir(parents=True, exist_ok=True)
        destination = directory / filename
        if not destination.exists():
            return destination
        return directory / f"{Path(filename).stem}_{uuid4().hex}.json"

    def _atomic_write(self, path, value):
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = path.parent / f".{path.name}.{uuid4().hex}.tmp"
        try:
            with temporary.open("x", encoding="utf-8", newline="\n") as stream:
                json.dump(
                    value,
                    stream,
                    ensure_ascii=False,
                    separators=(",", ":"),
                )
                stream.write("\n")
                stream.flush()
                os.fsync(stream.fileno())
            self._replace(temporary, path)
        except Exception:
            temporary.unlink(missing_ok=True)
            raise

    def _now(self):
        value = self._clock()
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("Outbox clock must return a timezone-aware value")
        return value.astimezone(timezone.utc)


def _validate_entry(entry):
    if not isinstance(entry, dict):
        raise ValueError("Outbox entry must be an object")
    if entry.get("schemaVersion") != OUTBOX_SCHEMA_VERSION:
        raise ValueError("Unsupported Outbox schemaVersion")
    for field_name in (
        "outboxId",
        "createdAt",
        "updatedAt",
        "nextRetryAt",
        "lastOutcome",
    ):
        if not isinstance(entry.get(field_name), str) or not entry[field_name]:
            raise ValueError(f"Outbox field {field_name} is required")
    retry_count = entry.get("retryCount")
    if (
        isinstance(retry_count, bool)
        or not isinstance(retry_count, int)
        or retry_count < 0
    ):
        raise ValueError("Outbox retryCount must be a non-negative integer")
    _parse_utc(entry["createdAt"])
    _parse_utc(entry["updatedAt"])
    _parse_utc(entry["nextRetryAt"])

    envelope = entry.get("envelope")
    if not isinstance(envelope, dict):
        raise ValueError("Outbox envelope must be an object")
    if envelope.get("schemaVersion") != 1:
        raise ValueError("Outbox envelope schemaVersion must be 1")
    for field_name in ("informationType", "source", "sourceItemId"):
        value = envelope.get(field_name)
        if not isinstance(value, str) or not value.strip():
            raise ValueError(f"Outbox envelope field {field_name} is required")


def _parse_utc(value):
    if not isinstance(value, str):
        raise ValueError("Outbox time must be a string")
    text = value[:-1] + "+00:00" if value.endswith("Z") else value
    try:
        parsed = datetime.fromisoformat(text)
    except ValueError as exception:
        raise ValueError("Outbox time must use ISO-8601") from exception
    if parsed.tzinfo is None or parsed.utcoffset() is None:
        raise ValueError("Outbox time must contain a timezone")
    return parsed.astimezone(timezone.utc)


def _format_utc(value):
    return (
        value.astimezone(timezone.utc)
        .isoformat(timespec="milliseconds")
        .replace("+00:00", "Z")
    )


def _outcome_text(value):
    if value is None:
        return "UNKNOWN"
    return str(getattr(value, "value", value))
