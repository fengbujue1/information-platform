"""Configuration for mapping and submitting InformationEnvelope V1."""

import configparser
import os
from dataclasses import dataclass
from dataclasses import field
from pathlib import Path
from typing import Callable, Mapping
from uuid import uuid4


DEFAULT_CONFIG_PATH = (
    Path(__file__).resolve().parent.parent / "config" / "collector.ini"
)


def _new_run_id():
    return str(uuid4())


@dataclass(frozen=True)
class MapperConfig:
    """Stable collector metadata and historical-time interpretation settings."""

    collector_id: str = "boss-collector-desktop"
    collector_version: str = "2.1.0"
    historical_timezone: str = "Asia/Shanghai"
    run_id_factory: Callable[[], str] = _new_run_id

    @classmethod
    def from_config_file(cls, config_path=None):
        """Load optional mapper settings from the shared Collector INI file."""
        parser = _read_config(config_path)
        section = _section(parser, "collector")
        return cls(
            collector_id=_value_or_default(
                section.get("collector_id"),
                cls.collector_id,
            ),
            collector_version=_value_or_default(
                section.get("collector_version"),
                cls.collector_version,
            ),
            historical_timezone=_value_or_default(
                section.get("historical_timezone"),
                cls.historical_timezone,
            ),
        )


@dataclass(frozen=True)
class HubClientConfig:
    """INI-backed settings with environment overrides for the optional Hub client."""

    enabled: bool = False
    url: str = ""
    token: str = field(default="", repr=False)
    connect_timeout_seconds: float = 3.0
    read_timeout_seconds: float = 10.0

    @classmethod
    def from_environment(cls, environ: Mapping[str, str] | None = None):
        """Build client settings without requiring Hub variables while disabled."""
        return cls._from_values({}, environ)

    @classmethod
    def from_sources(
        cls,
        config_path=None,
        environ: Mapping[str, str] | None = None,
    ):
        """Load INI settings and let explicitly present environment values win."""
        parser = _read_config(config_path)
        return cls._from_values(
            _section(parser, "information_hub"),
            environ,
        )

    @classmethod
    def _from_values(
        cls,
        file_values: Mapping[str, str],
        environ: Mapping[str, str] | None,
    ):
        values = os.environ if environ is None else environ
        enabled = _parse_boolean(
            _preferred_value(
                values,
                "INFORMATION_HUB_ENABLED",
                file_values.get("enabled"),
            ),
            default=False,
        )
        if not enabled:
            return cls(enabled=False)
        return cls(
            enabled=True,
            url=_optional_text(
                _preferred_value(
                    values,
                    "INFORMATION_HUB_URL",
                    file_values.get("url"),
                )
            ),
            token=_optional_text(
                _preferred_value(
                    values,
                    "INFORMATION_HUB_COLLECTOR_TOKEN",
                    file_values.get("collector_token"),
                )
            ),
            connect_timeout_seconds=_parse_positive_float(
                _preferred_value(
                    values,
                    "INFORMATION_HUB_CONNECT_TIMEOUT_SECONDS",
                    file_values.get("connect_timeout_seconds"),
                ),
                default=3.0,
                name="INFORMATION_HUB_CONNECT_TIMEOUT_SECONDS",
            ),
            read_timeout_seconds=_parse_positive_float(
                _preferred_value(
                    values,
                    "INFORMATION_HUB_READ_TIMEOUT_SECONDS",
                    file_values.get("read_timeout_seconds"),
                ),
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


def _read_config(config_path):
    explicit_path = config_path is not None
    path = Path(config_path) if explicit_path else DEFAULT_CONFIG_PATH
    parser = configparser.ConfigParser(interpolation=None)
    if not path.exists():
        if explicit_path:
            raise ValueError("Collector config file does not exist")
        return parser
    if not path.is_file():
        raise ValueError("Collector config path must be a file")
    try:
        with path.open(encoding="utf-8") as stream:
            parser.read_file(stream)
    except (OSError, UnicodeError, configparser.Error) as exception:
        raise ValueError("Collector config file could not be read") from exception
    return parser


def _section(parser, name):
    if not parser.has_section(name):
        return {}
    return dict(parser.items(name))


def _preferred_value(environ, name, file_value):
    if name in environ:
        return environ[name]
    return file_value


def _value_or_default(value, default):
    text = _optional_text(value)
    return text or default


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
