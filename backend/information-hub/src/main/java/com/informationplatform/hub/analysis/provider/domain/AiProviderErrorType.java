package com.informationplatform.hub.analysis.provider.domain;

/** Provider 调用失败的稳定平台分类。 */
public enum AiProviderErrorType {
    DISABLED,
    CONFIGURATION,
    AUTHENTICATION,
    RATE_LIMITED,
    UPSTREAM_CLIENT_ERROR,
    UPSTREAM_SERVER_ERROR,
    TIMEOUT,
    NETWORK,
    INVALID_RESPONSE
}
