package com.informationplatform.hub.recommendation.run.domain;

/** Worker 原子完成后的 Run 终态与结果数量。 */
public record RecommendationRunCompletion(
        /** COMPLETED 或 NOOP。 */ String status,
        /** 原子可见的 Recommendation Item 数量。 */ int resultCount) {

    public RecommendationRunCompletion {
        if (!("COMPLETED".equals(status) || "NOOP".equals(status))
                || resultCount < 0
                || ("NOOP".equals(status) && resultCount != 0)) {
            throw new IllegalArgumentException("Recommendation Run completion is invalid");
        }
    }
}
