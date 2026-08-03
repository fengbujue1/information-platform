package com.informationplatform.hub.analysis.provider.domain;

public record AiProviderResult(
        /** 稳定 Provider 标识。 */
        String provider,
        /** Provider 实际返回或配置使用的模型名。 */
        String modelName,
        /** Provider 返回的请求 ID；未返回时为 null。 */
        String providerRequestId,
        /** Assistant 输出原文，由 TASK-026 负责 JSON/Schema 后处理。 */
        String outputText,
        /** Provider 返回的结束原因；未返回时为 null。 */
        String finishReason,
        /** 客户端观测请求耗时，单位毫秒。 */
        long latencyMs,
        /** Provider 实际报告的 Usage，缺失时为 UNAVAILABLE。 */
        AiProviderUsage usage) {

    public AiProviderResult {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("AI Provider id must not be blank");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("AI Provider model name must not be blank");
        }
        if (outputText == null) {
            throw new IllegalArgumentException("AI Provider output text must not be null");
        }
        if (latencyMs < 0) {
            throw new IllegalArgumentException("AI Provider latency must not be negative");
        }
        if (usage == null) {
            throw new IllegalArgumentException("AI Provider usage must not be null");
        }
    }
}
