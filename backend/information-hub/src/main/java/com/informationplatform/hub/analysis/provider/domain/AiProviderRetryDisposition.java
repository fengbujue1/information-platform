package com.informationplatform.hub.analysis.provider.domain;

/** 供后续 Worker 判断的安全重试处置；Provider Client 本身不执行重试。 */
public enum AiProviderRetryDisposition {
    NEVER,
    RETRY_WITH_BACKOFF,
    AMBIGUOUS_DO_NOT_AUTO_RETRY
}
