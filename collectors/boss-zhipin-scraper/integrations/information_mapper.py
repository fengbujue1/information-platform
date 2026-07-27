"""Map BOSS list and detail output to InformationEnvelope V1 dictionaries."""

import copy
import logging
import re
from datetime import datetime, timedelta, timezone
from decimal import Decimal, InvalidOperation
from typing import Any, Mapping
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from .boss_job_merger import merge_jobs_with_details
from .config import MapperConfig


LOGGER = logging.getLogger(__name__)

_DETAIL_STATUSES = {"UNKNOWN", "FETCHED", "FAILED", "UNAVAILABLE"}
_DEGREE_TAGS = {
    "初中及以下",
    "中专/中技",
    "高中",
    "大专",
    "本科",
    "硕士",
    "博士",
    "学历不限",
}
_EXPERIENCE_TAGS = {"应届", "应届生", "在校生", "经验不限", "不限经验", "无经验"}
_EXPERIENCE_PATTERN = re.compile(
    r"^(?:\d+(?:[-~～—–]\d+)?年(?:以内|以上)?|10年\+)$"
)
_SALARY_PATTERN = re.compile(
    r"^\s*(\d+(?:\.\d+)?)\s*[-~～—–]\s*(\d+(?:\.\d+)?)\s*[Kk]"
    r"(?:\s*[·・]\s*(\d+)(?:\s*[-~～—–]\s*\d+)?\s*薪)?\s*$"
)
_FORBIDDEN_RAW_KEYS = {
    "authorization",
    "authorizationheader",
    "browsercredentials",
    "browserlocalcredentials",
    "chromeprofile",
    "cookie",
    "cookies",
    "credentials",
    "lid",
    "logintoken",
    "password",
    "refreshtoken",
    "securityid",
    "token",
    "accesstoken",
}


class BossMappingError(ValueError):
    """Raised when source data cannot satisfy InformationEnvelope V1."""


def map_boss_results(list_root, detail_source=None, config=None, logger=None):
    """Map one BOSS search batch; every list item produces one envelope."""
    config = config or MapperConfig()
    logger = logger or LOGGER
    if not isinstance(list_root, Mapping):
        raise BossMappingError("BOSS list root must be an object")
    jobs = list_root.get("jobs")
    if not isinstance(jobs, list):
        raise BossMappingError("BOSS list root must contain a jobs array")

    collected_at, source_scraped_at, timezone_assumed = _source_times(
        list_root.get("scraped_at"),
        config.historical_timezone,
    )
    run_id = _optional_text(list_root.get("run_id"))
    if run_id is None:
        run_id = _optional_text(config.run_id_factory())
    if run_id is None:
        raise BossMappingError("Mapper run_id must not be empty")

    context = _collection_context(
        list_root,
        run_id,
        source_scraped_at,
        config.historical_timezone if timezone_assumed else None,
        len(jobs),
    )
    try:
        merged_jobs = merge_jobs_with_details(jobs, detail_source, logger)
    except ValueError as exception:
        raise BossMappingError(str(exception)) from exception

    return [
        _map_job(
            merged.list_item,
            merged.detail,
            collected_at,
            context,
            config,
            logger,
        )
        for merged in merged_jobs
    ]


def _map_job(job, detail, collected_at, context, config, logger):
    source_item_id = _required_text(job.get("encrypt_job_id"), "encrypt_job_id")
    title = _required_text(job.get("title"), "title")
    salary_text = _optional_text(job.get("salary"))
    salary_minimum, salary_maximum, salary_months = _parse_salary(salary_text)
    location_name = _optional_text(job.get("location"))
    city_name, area_name, district_name = _parse_location(location_name)
    source_tags = _split_values(job.get("tags"))
    experience_text, education_text = _recognize_requirements(source_tags)
    content = _optional_text(detail.get("jd")) if detail is not None else None
    detail_status = _detail_status(detail, content, logger)
    detail_collected_at = _explicit_source_time(
        detail.get("detail_collected_at") if detail is not None else None,
        "detail_collected_at",
        logger,
    )
    recruiter_active_text = _recruiter_active_text(job, logger)

    raw_payload = {
        "list": _sanitize_payload(job),
        "detail": _sanitize_payload(detail) if detail is not None else None,
    }
    return {
        "schemaVersion": 1,
        "informationType": "JOB",
        "source": "BOSS",
        "sourceItemId": source_item_id,
        "sourceUrl": _optional_text(job.get("job_link")),
        "title": title,
        "content": content,
        "publishTime": None,
        "collectedAt": collected_at,
        "collector": {
            "collectorId": _required_text(config.collector_id, "collector_id"),
            "collectorVersion": _required_text(
                config.collector_version,
                "collector_version",
            ),
        },
        "collectionContext": copy.deepcopy(context),
        "extension": {
            "sourceCompanyId": _optional_text(job.get("encrypt_brand_id")),
            "sourceRecruiterId": _optional_text(job.get("encrypt_boss_id")),
            "companyName": _optional_text(job.get("boss_name")),
            "companyUrl": _optional_text(job.get("company_link")),
            "companyScaleText": _optional_text(job.get("company_scale")),
            "companyStageText": _optional_text(job.get("company_stage")),
            "companyIndustryText": _optional_text(job.get("company_industry")),
            "salaryText": salary_text,
            "salarySource": _upper_optional_text(job.get("salary_source")),
            "salaryMinMonthlyYuan": salary_minimum,
            "salaryMaxMonthlyYuan": salary_maximum,
            "salaryMonths": salary_months,
            "locationName": location_name,
            "cityName": city_name,
            "areaName": area_name,
            "businessDistrictName": district_name,
            "experienceText": experience_text,
            "educationText": education_text,
            "recruiterName": None,
            "recruiterTitle": _optional_text(job.get("boss_title")),
            "recruiterActiveText": recruiter_active_text,
            "remoteType": "UNKNOWN",
            "jobStatus": "ACTIVE",
            "detailStatus": detail_status,
            "detailCollectedAt": detail_collected_at,
            "sourceTags": source_tags or None,
            "sourceSkillTags": _split_values(job.get("skills")) or None,
            "welfare": _split_values(job.get("welfare")) or None,
        },
        "rawPayload": raw_payload,
    }


def _collection_context(
        list_root,
        run_id,
        source_scraped_at,
        timezone_assumption,
        jobs_count,
):
    filters = list_root.get("filters")
    if not isinstance(filters, Mapping):
        filters = {}
    total = list_root.get("total")
    if isinstance(total, bool) or not isinstance(total, int) or total < 0:
        total = jobs_count
    context = {
        "runId": run_id,
        "keyword": _optional_text(list_root.get("keyword")),
        "city": _optional_text(list_root.get("city")),
        "queryFilters": _sanitize_payload(filters),
        "filterDescriptions": _split_values(list_root.get("filter_desc")),
        "resultTotal": total,
        "sourceScrapedAt": source_scraped_at,
    }
    if timezone_assumption is not None:
        context["timeZoneAssumption"] = timezone_assumption
    return context


def _source_times(value, historical_timezone):
    text = _required_text(value, "scraped_at")
    parsed = _parse_iso_datetime(text, "scraped_at")
    timezone_assumed = parsed.tzinfo is None or parsed.utcoffset() is None
    if timezone_assumed:
        parsed = parsed.replace(tzinfo=_timezone_from_name(historical_timezone))
    return (
        _iso_utc(parsed),
        _iso_with_offset(parsed),
        timezone_assumed,
    )


def _explicit_source_time(value, field_name, logger):
    text = _optional_text(value)
    if text is None:
        return None
    try:
        parsed = _parse_iso_datetime(text, field_name)
    except BossMappingError:
        logger.warning("Ignoring invalid BOSS time field=%s", field_name)
        return None
    if parsed.tzinfo is None or parsed.utcoffset() is None:
        logger.warning("Ignoring timezone-free BOSS time field=%s", field_name)
        return None
    return text


def _recruiter_active_text(job, logger):
    if job.get("boss_online") is not True:
        return None
    return _explicit_source_time(
        job.get("boss_online_observed_at"),
        "boss_online_observed_at",
        logger,
    )


def _detail_status(detail, content, logger):
    if detail is None:
        return "UNKNOWN"
    explicit = _upper_optional_text(detail.get("detail_status"))
    if explicit is not None and explicit not in _DETAIL_STATUSES:
        logger.warning("Ignoring unsupported BOSS detail_status=%s", explicit)
        explicit = None
    if content is not None:
        if explicit not in (None, "FETCHED"):
            logger.warning(
                "BOSS detail contains JD but status=%s; FETCHED is used",
                explicit,
            )
        return "FETCHED"
    if explicit == "FETCHED":
        logger.warning("BOSS detail status is FETCHED but JD is empty; UNKNOWN is used")
        return "UNKNOWN"
    return explicit or "UNKNOWN"


def _parse_salary(value):
    if value is None:
        return None, None, None
    match = _SALARY_PATTERN.fullmatch(value)
    if match is None:
        return None, None, None
    try:
        minimum = int(Decimal(match.group(1)) * 1000)
        maximum = int(Decimal(match.group(2)) * 1000)
    except (InvalidOperation, ValueError):
        return None, None, None
    months = int(match.group(3)) if match.group(3) is not None else None
    return minimum, maximum, months


def _parse_location(value):
    if value is None:
        return None, None, None
    parts = [part.strip() or None for part in value.split("·")]
    city = parts[0] if parts else None
    area = parts[1] if len(parts) > 1 else None
    district = parts[2] if len(parts) > 2 else None
    return city, area, district


def _recognize_requirements(tags):
    experience = next((tag for tag in tags if _is_experience_tag(tag)), None)
    education = next((tag for tag in tags if tag in _DEGREE_TAGS), None)
    return experience, education


def _is_experience_tag(tag):
    return tag in _EXPERIENCE_TAGS or _EXPERIENCE_PATTERN.fullmatch(tag) is not None


def _split_values(value):
    if value is None:
        return []
    if isinstance(value, (list, tuple)):
        candidates = value
    else:
        candidates = str(value).replace("｜", "|").split("|")
    result = []
    seen = set()
    for candidate in candidates:
        text = _optional_text(candidate)
        if text is not None and text not in seen:
            seen.add(text)
            result.append(text)
    return result


def _sanitize_payload(value):
    if isinstance(value, Mapping):
        sanitized = {}
        for key, nested in value.items():
            key_text = str(key)
            if _normalize_key(key_text) in _FORBIDDEN_RAW_KEYS:
                continue
            sanitized[key_text] = _sanitize_payload(nested)
        return sanitized
    if isinstance(value, list):
        return [_sanitize_payload(item) for item in value]
    if isinstance(value, tuple):
        return [_sanitize_payload(item) for item in value]
    return copy.deepcopy(value)


def _normalize_key(key):
    return re.sub(r"[^a-z0-9]", "", key.lower())


def _parse_iso_datetime(value, field_name):
    normalized = value[:-1] + "+00:00" if value.endswith(("Z", "z")) else value
    try:
        return datetime.fromisoformat(normalized)
    except ValueError as exception:
        raise BossMappingError(f"BOSS {field_name} must be ISO-8601") from exception


def _timezone_from_name(name):
    if name == "Asia/Shanghai":
        return timezone(timedelta(hours=8), name)
    if name in {"UTC", "Etc/UTC", "Z"}:
        return timezone.utc
    try:
        return ZoneInfo(name)
    except ZoneInfoNotFoundError as exception:
        raise BossMappingError(f"Unknown historical timezone: {name}") from exception


def _iso_utc(value):
    return value.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def _iso_with_offset(value):
    return value.isoformat().replace("+00:00", "Z")


def _required_text(value, field_name):
    text = _optional_text(value)
    if text is None:
        raise BossMappingError(f"BOSS {field_name} must not be empty")
    return text


def _optional_text(value):
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _upper_optional_text(value):
    text = _optional_text(value)
    return text.upper() if text is not None else None
