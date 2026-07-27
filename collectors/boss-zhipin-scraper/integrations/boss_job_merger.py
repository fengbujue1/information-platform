"""Left-join BOSS list jobs with detail records without changing source data."""

import logging
from dataclasses import dataclass
from typing import Any, Mapping, Sequence


LOGGER = logging.getLogger(__name__)

_CONFLICT_FIELDS = (
    ("title", "title"),
    ("boss_name", "company"),
    ("salary", "salary"),
    ("location", "location"),
    ("tags", "tags_list"),
    ("job_link", "job_link"),
)


@dataclass(frozen=True)
class MergedBossJob:
    """A list item and its optional detail, associated only by local job_id."""

    list_item: Mapping[str, Any]
    detail: Mapping[str, Any] | None


def extract_details(detail_source):
    """Accept the historical bare array and the newer detail-root object."""
    if detail_source is None:
        return []
    if isinstance(detail_source, Mapping):
        details = detail_source.get("details", [])
    else:
        details = detail_source
    if not isinstance(details, Sequence) or isinstance(details, (str, bytes)):
        raise ValueError("BOSS details must be an array or an object containing details")
    return list(details)


def merge_jobs_with_details(jobs, detail_source, logger=None):
    """Left-join details and warn on duplicates, orphans, and field conflicts."""
    logger = logger or LOGGER
    if not isinstance(jobs, Sequence) or isinstance(jobs, (str, bytes)):
        raise ValueError("BOSS jobs must be an array")

    detail_index = {}
    for index, detail in enumerate(extract_details(detail_source)):
        if not isinstance(detail, Mapping):
            logger.warning("Ignoring non-object BOSS detail at index=%s", index)
            continue
        job_id = _optional_text(detail.get("job_id"))
        if job_id is None:
            logger.warning("Ignoring BOSS detail without job_id at index=%s", index)
            continue
        if job_id in detail_index:
            logger.warning(
                "Duplicate BOSS detail job_id=%s; the last detail is used",
                job_id,
            )
        detail_index[job_id] = detail

    list_job_ids = {
        job_id
        for job in jobs
        if isinstance(job, Mapping)
        if (job_id := _optional_text(job.get("job_id"))) is not None
    }
    for orphan_job_id in detail_index.keys() - list_job_ids:
        logger.warning(
            "Ignoring unmatched BOSS detail job_id=%s",
            orphan_job_id,
        )

    merged = []
    for index, job in enumerate(jobs):
        if not isinstance(job, Mapping):
            raise ValueError(f"BOSS job at index {index} must be an object")
        job_id = _optional_text(job.get("job_id"))
        detail = detail_index.get(job_id) if job_id is not None else None
        if detail is not None:
            _warn_conflicts(job_id, job, detail, logger)
        merged.append(MergedBossJob(job, detail))
    return merged


def _warn_conflicts(job_id, job, detail, logger):
    for list_field, detail_field in _CONFLICT_FIELDS:
        list_value = _optional_text(job.get(list_field))
        detail_value = _optional_text(detail.get(detail_field))
        if list_value is not None and detail_value is not None and list_value != detail_value:
            logger.warning(
                "BOSS list/detail conflict job_id=%s field=%s; list value is kept",
                job_id,
                list_field,
            )


def _optional_text(value):
    if value is None:
        return None
    text = str(value).strip()
    return text or None
