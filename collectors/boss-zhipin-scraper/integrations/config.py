"""Configuration for mapping and submitting InformationEnvelope V1."""

import os
from dataclasses import dataclass
from dataclasses import field
from typing import Callable, Mapping
from uuid import uuid4


def _new_run_id():
    return str(uuid4())


@dataclass(frozen=True)
class MapperConfig:
    """Stable collector metadata and historical-time interpretation settings."""

    collector_id: str = "boss-collector-desktop"
    collector_version: str = "2.1.0"
    historical_timezone: str = "Asia/Shanghai"
    run_id_factory: Callable[[], str] = _new_run_id


@dataclass(frozen=True)
class HubClientConfig:
    """Environment-backed settings for the optional Information Hub client."""

    enabled: bool = False
    url: str = ""
    token: str = field(default="", repr=False)
    connect_timeout_seconds: float = 3.0
    read_timeout_seconds: float = 10.0

    @classmethod
    def from_environment(cls, environ: Mapping[str, str] | None = None):
        """Build client settings without requiring Hub variables while disabled."""
        values = os.environ if environ is None else environ
        enabled = _parse_boolean(
            values.get("INFORMATION_HUB_ENABLED"),
            default=False,
        )
        if not enabled:
            return cls(enabled=False)
        return cls(
            enabled=True,
            url=_optional_text(values.get("INFORMATION_HUB_URL")),
            token=_optional_text(
                values.get("INFORMATION_HUB_COLLECTOR_TOKEN")
            ),
            connect_timeout_seconds=_parse_positive_float(
                values.get("INFORMATION_HUB_CONNECT_TIMEOUT_SECONDS"),
                default=3.0,
                name="INFORMATION_HUB_CONNECT_TIMEOUT_SECONDS",
            ),
            read_timeout_seconds=_parse_positive_float(
                values.get("INFORMATION_HUB_READ_TIMEOUT_SECONDS"),
                default=10.0,
                name="INFORMATION_HUB_READ_TIMEOUT_SECONDS",
            ),
        )

    def validation_error(self):
        """Return a safe configuration error that never contains the token."""
        if not self.enabled:
            return None
        if not self.url:
            return "INFORMATION_HUB_URL is required when Hub submission is enabled"
        if not self.url.startswith(("http://", "https://")):
            return "INFORMATION_HUB_URL must use http or https"
        if not self.token:
            return (
                "INFORMATION_HUB_COLLECTOR_TOKEN is required when "
                "Hub submission is enabled"
            )
        return None


def _parse_boolean(value, default):
    text = _optional_text(value).lower()
    if not text:
        return default
    if text in {"1", "true", "yes", "on"}:
        return True
    if text in {"0", "false", "no", "off"}:
        return False
    raise ValueError("INFORMATION_HUB_ENABLED must be a boolean")


def _parse_positive_float(value, default, name):
    text = _optional_text(value)
    if not text:
        return default
    try:
        parsed = float(text)
    except ValueError as exception:
        raise ValueError(f"{name} must be a positive number") from exception
    if parsed <= 0:
        raise ValueError(f"{name} must be a positive number")
    return parsed


def _optional_text(value):
    if value is None:
        return ""
    return str(value).strip()
