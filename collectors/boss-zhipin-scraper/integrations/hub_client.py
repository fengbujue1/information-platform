"""Best-effort Information Hub HTTP submission for mapped BOSS jobs."""

import logging
from dataclasses import dataclass
from enum import Enum
from typing import Any, Mapping

import requests

from .config import HubClientConfig, MapperConfig
from .information_mapper import map_boss_results


LOGGER = logging.getLogger(__name__)


class HubSubmitOutcome(str, Enum):
    """Stable outcomes returned by a single Information Hub request."""

    DISABLED = "DISABLED"
    CONFIGURATION_ERROR = "CONFIGURATION_ERROR"
    SUCCESS = "SUCCESS"
    CLIENT_ERROR = "CLIENT_ERROR"
    PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE"
    SERVER_ERROR = "SERVER_ERROR"
    HTTP_ERROR = "HTTP_ERROR"
    TIMEOUT = "TIMEOUT"
    CONNECTION_FAILED = "CONNECTION_FAILED"
    REQUEST_FAILED = "REQUEST_FAILED"


@dataclass(frozen=True)
class HubSubmitResult:
    """Result of submitting one InformationEnvelope."""

    outcome: HubSubmitOutcome
    status_code: int | None = None

    @property
    def succeeded(self):
        return self.outcome is HubSubmitOutcome.SUCCESS

    @property
    def should_stop_batch(self):
        return self.outcome in {
            HubSubmitOutcome.CONFIGURATION_ERROR,
            HubSubmitOutcome.SERVER_ERROR,
            HubSubmitOutcome.HTTP_ERROR,
            HubSubmitOutcome.TIMEOUT,
            HubSubmitOutcome.CONNECTION_FAILED,
            HubSubmitOutcome.REQUEST_FAILED,
        }


@dataclass(frozen=True)
class HubBatchResult:
    """Summary of one optional best-effort submission batch."""

    enabled: bool
    total: int = 0
    succeeded: int = 0
    failed: int = 0
    skipped: int = 0
    configuration_valid: bool = True
    mapping_succeeded: bool = True


class InformationHubClient:
    """Submit one InformationEnvelope without exposing credentials in logs."""

    def __init__(self, config, session=None, logger=None):
        self._config = config
        self._session = session or requests.Session()
        self._logger = logger or LOGGER

    def submit(self, envelope: Mapping[str, Any]):
        """POST one envelope and classify HTTP and transport failures."""
        if not self._config.enabled:
            return HubSubmitResult(HubSubmitOutcome.DISABLED)
        configuration_error = self._config.validation_error()
        if configuration_error is not None:
            self._logger.warning(
                "Information Hub configuration is invalid: %s",
                configuration_error,
            )
            return HubSubmitResult(HubSubmitOutcome.CONFIGURATION_ERROR)
        headers = {
            "Authorization": f"Bearer {self._config.token}",
            "Accept": "application/json",
        }
        timeout = (
            self._config.connect_timeout_seconds,
            self._config.read_timeout_seconds,
        )
        try:
            response = self._session.post(
                self._config.url,
                json=envelope,
                headers=headers,
                timeout=timeout,
            )
        except requests.Timeout:
            self._logger.warning("Information Hub request timed out")
            return HubSubmitResult(HubSubmitOutcome.TIMEOUT)
        except requests.ConnectionError:
            self._logger.warning("Information Hub connection failed")
            return HubSubmitResult(HubSubmitOutcome.CONNECTION_FAILED)
        except requests.RequestException:
            self._logger.warning("Information Hub request failed")
            return HubSubmitResult(HubSubmitOutcome.REQUEST_FAILED)

        status_code = response.status_code
        if 200 <= status_code < 300:
            self._logger.info(
                "Information Hub accepted item status=%s",
                status_code,
            )
            return HubSubmitResult(HubSubmitOutcome.SUCCESS, status_code)
        if status_code == 413:
            self._logger.warning(
                "Information Hub rejected oversized item status=413"
            )
            return HubSubmitResult(
                HubSubmitOutcome.PAYLOAD_TOO_LARGE,
                status_code,
            )
        if 400 <= status_code < 500:
            self._logger.warning(
                "Information Hub rejected item status=%s",
                status_code,
            )
            return HubSubmitResult(
                HubSubmitOutcome.CLIENT_ERROR,
                status_code,
            )
        if 500 <= status_code < 600:
            self._logger.warning(
                "Information Hub service failed status=%s",
                status_code,
            )
            return HubSubmitResult(
                HubSubmitOutcome.SERVER_ERROR,
                status_code,
            )
        self._logger.warning(
            "Information Hub returned unexpected status=%s",
            status_code,
        )
        return HubSubmitResult(HubSubmitOutcome.HTTP_ERROR, status_code)


def submit_boss_results_to_hub(
    list_root,
    detail_source=None,
    *,
    config_path=None,
    environ=None,
    client_config=None,
    mapper_config=None,
    session=None,
    logger=None,
):
    """Map and submit a saved BOSS batch without interrupting collection."""
    logger = logger or LOGGER
    try:
        return _submit_boss_results_to_hub(
            list_root,
            detail_source,
            config_path=config_path,
            environ=environ,
            client_config=client_config,
            mapper_config=mapper_config,
            session=session,
            logger=logger,
        )
    except Exception as exception:
        # This is the final collector boundary: log only the exception type so
        # payload values and credentials cannot leak through exception text.
        logger.warning(
            "Information Hub submission was skipped after unexpected "
            "failure type=%s",
            type(exception).__name__,
        )
        return HubBatchResult(
            enabled=True,
            failed=1,
            configuration_valid=False,
            mapping_succeeded=False,
        )


def _submit_boss_results_to_hub(
    list_root,
    detail_source,
    *,
    config_path,
    environ,
    client_config,
    mapper_config,
    session,
    logger,
):
    try:
        config = client_config or HubClientConfig.from_sources(
            config_path,
            environ,
        )
    except ValueError:
        logger.warning("Information Hub configuration is invalid")
        return HubBatchResult(enabled=True, configuration_valid=False)

    if not config.enabled:
        logger.debug("Information Hub submission is disabled")
        return HubBatchResult(enabled=False)

    configuration_error = config.validation_error()
    if configuration_error is not None:
        logger.warning(
            "Information Hub configuration is invalid: %s",
            configuration_error,
        )
        return HubBatchResult(enabled=True, configuration_valid=False)

    try:
        envelopes = map_boss_results(
            list_root,
            detail_source,
            mapper_config or MapperConfig.from_config_file(config_path),
            logger,
        )
    except Exception as exception:
        logger.warning(
            "Information Hub mapping failed type=%s",
            type(exception).__name__,
        )
        return HubBatchResult(enabled=True, mapping_succeeded=False)

    client = InformationHubClient(config, session=session, logger=logger)
    succeeded = 0
    failed = 0
    skipped = 0
    for index, envelope in enumerate(envelopes):
        try:
            result = client.submit(envelope)
        except Exception as exception:
            logger.warning(
                "Information Hub client failed unexpectedly type=%s",
                type(exception).__name__,
            )
            failed += 1
            skipped = len(envelopes) - index - 1
            break
        if result.succeeded:
            succeeded += 1
        else:
            failed += 1
        if result.should_stop_batch:
            skipped = len(envelopes) - index - 1
            break

    logger.info(
        "Information Hub submission completed total=%s succeeded=%s "
        "failed=%s skipped=%s",
        len(envelopes),
        succeeded,
        failed,
        skipped,
    )
    return HubBatchResult(
        enabled=True,
        total=len(envelopes),
        succeeded=succeeded,
        failed=failed,
        skipped=skipped,
    )
