"""Information Platform adapters for the BOSS collector."""

from .config import MapperConfig
from .information_mapper import BossMappingError, map_boss_results

__all__ = ["BossMappingError", "MapperConfig", "map_boss_results"]
