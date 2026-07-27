"""Configuration for mapping BOSS output to InformationEnvelope V1."""

from dataclasses import dataclass
from typing import Callable
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
