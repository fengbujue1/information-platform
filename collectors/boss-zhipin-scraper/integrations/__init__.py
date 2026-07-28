"""Information Platform adapters for the BOSS collector."""

from .config import HubClientConfig, MapperConfig
from .hub_client import (
    HubBatchResult,
    HubOutboxFlushResult,
    HubSubmitOutcome,
    HubSubmitResult,
    InformationHubClient,
    flush_information_hub_outbox,
    submit_boss_results_to_hub,
)
from .information_mapper import BossMappingError, map_boss_results
from .outbox import FileOutbox, OutboxFlushResult

__all__ = [
    "BossMappingError",
    "FileOutbox",
    "HubBatchResult",
    "HubClientConfig",
    "HubOutboxFlushResult",
    "HubSubmitOutcome",
    "HubSubmitResult",
    "InformationHubClient",
    "MapperConfig",
    "OutboxFlushResult",
    "flush_information_hub_outbox",
    "map_boss_results",
    "submit_boss_results_to_hub",
]
