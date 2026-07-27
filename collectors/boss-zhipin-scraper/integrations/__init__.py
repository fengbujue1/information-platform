"""Information Platform adapters for the BOSS collector."""

from .config import HubClientConfig, MapperConfig
from .hub_client import (
    HubBatchResult,
    HubSubmitOutcome,
    HubSubmitResult,
    InformationHubClient,
    submit_boss_results_to_hub,
)
from .information_mapper import BossMappingError, map_boss_results

__all__ = [
    "BossMappingError",
    "HubBatchResult",
    "HubClientConfig",
    "HubSubmitOutcome",
    "HubSubmitResult",
    "InformationHubClient",
    "MapperConfig",
    "map_boss_results",
    "submit_boss_results_to_hub",
]
