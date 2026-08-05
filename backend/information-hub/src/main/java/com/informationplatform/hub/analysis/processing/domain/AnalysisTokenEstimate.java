package com.informationplatform.hub.analysis.processing.domain;

/** 一次 Analysis 调用前的 Token 预算估算，不代表 Provider Actual Usage。 */
public record AnalysisTokenEstimate(
        /** 预估输入 Token 数。 */
        long inputTokens,
        /** Definition 冻结的最大输出 Token 数。 */
        long outputTokens,
        /** 预估输入与输出 Token 总数。 */
        long totalTokens,
        /** 可审计的估算算法版本。 */
        String method) {

    public AnalysisTokenEstimate {
        if (inputTokens < 0
                || outputTokens < 0
                || totalTokens != inputTokens + outputTokens
                || method == null
                || method.isBlank()) {
            throw new IllegalArgumentException("Analysis token estimate is invalid");
        }
    }
}
