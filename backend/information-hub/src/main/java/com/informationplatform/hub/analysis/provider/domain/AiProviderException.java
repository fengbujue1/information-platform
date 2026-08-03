package com.informationplatform.hub.analysis.provider.domain;

/** Provider 调用失败；消息必须稳定且不得包含响应正文、API Key 或 Authorization。 */
public class AiProviderException extends RuntimeException {

    /** 稳定失败分类。 */
    private final AiProviderErrorType errorType;
    /** 后续调用方可采用的安全重试处置。 */
    private final AiProviderRetryDisposition retryDisposition;
    /** HTTP 状态码；非 HTTP 错误时为 null。 */
    private final Integer httpStatus;
    /** Provider 返回的请求 ID；无法获取时为 null。 */
    private final String providerRequestId;
    /** 失败前已解析到的 Usage；未报告时为 UNAVAILABLE。 */
    private final AiProviderUsage usage;
    /** 客户端观测请求耗时，单位毫秒。 */
    private final long latencyMs;

    public AiProviderException(
            AiProviderErrorType errorType,
            AiProviderRetryDisposition retryDisposition,
            String message,
            Integer httpStatus,
            String providerRequestId,
            AiProviderUsage usage,
            long latencyMs,
            Throwable cause) {
        super(message, cause);
        if (errorType == null || retryDisposition == null || usage == null || latencyMs < 0) {
            throw new IllegalArgumentException("AI Provider exception metadata is invalid");
        }
        this.errorType = errorType;
        this.retryDisposition = retryDisposition;
        this.httpStatus = httpStatus;
        this.providerRequestId = providerRequestId;
        this.usage = usage;
        this.latencyMs = latencyMs;
    }

    public AiProviderErrorType errorType() {
        return errorType;
    }

    public AiProviderRetryDisposition retryDisposition() {
        return retryDisposition;
    }

    public Integer httpStatus() {
        return httpStatus;
    }

    public String providerRequestId() {
        return providerRequestId;
    }

    public AiProviderUsage usage() {
        return usage;
    }

    public long latencyMs() {
        return latencyMs;
    }
}
