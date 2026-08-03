package com.informationplatform.hub.analysis.provider.domain;

public record AiProviderUsage(
        /** Provider Usage 报告的输入 Token；未报告时为 null。 */
        Long inputTokens,
        /** Provider Usage 报告的输出 Token；未报告时为 null。 */
        Long outputTokens,
        /** Provider Usage 报告的总 Token；未报告时为 null。 */
        Long totalTokens,
        /** Provider Usage 报告的缓存输入 Token；未报告时为 null。 */
        Long cachedInputTokens,
        /** Provider Usage 报告的推理 Token；未报告时为 null。 */
        Long reasoningTokens,
        /** Usage 报告状态。 */
        AiProviderUsageStatus status) {

    public AiProviderUsage {
        if (status == null) {
            throw new IllegalArgumentException("AI Provider usage status must not be null");
        }
        requireNonNegative(inputTokens, "inputTokens");
        requireNonNegative(outputTokens, "outputTokens");
        requireNonNegative(totalTokens, "totalTokens");
        requireNonNegative(cachedInputTokens, "cachedInputTokens");
        requireNonNegative(reasoningTokens, "reasoningTokens");
        if (status == AiProviderUsageStatus.UNAVAILABLE
                && (inputTokens != null
                        || outputTokens != null
                        || totalTokens != null
                        || cachedInputTokens != null
                        || reasoningTokens != null)) {
            throw new IllegalArgumentException("Unavailable usage must not contain token values");
        }
        if (status == AiProviderUsageStatus.REPORTED
                && inputTokens == null
                && outputTokens == null
                && totalTokens == null
                && cachedInputTokens == null
                && reasoningTokens == null) {
            throw new IllegalArgumentException("Reported usage must contain at least one token value");
        }
    }

    public static AiProviderUsage unavailable() {
        return new AiProviderUsage(null, null, null, null, null, AiProviderUsageStatus.UNAVAILABLE);
    }

    public static AiProviderUsage reported(
            Long inputTokens,
            Long outputTokens,
            Long totalTokens,
            Long cachedInputTokens,
            Long reasoningTokens) {
        return new AiProviderUsage(
                inputTokens,
                outputTokens,
                totalTokens,
                cachedInputTokens,
                reasoningTokens,
                AiProviderUsageStatus.REPORTED);
    }

    private static void requireNonNegative(Long value, String field) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }
}
