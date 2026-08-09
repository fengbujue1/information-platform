package com.informationplatform.hub.recommendation.run.domain;

/** Manual Refresh 成功冻结并持久化的 PENDING Run 身份。 */
public record RecommendationRunAccepted(
        /** 新建 Recommendation Run 主键。 */ long runId,
        /** 创建后的初始状态，固定为 PENDING。 */ String status) {

    public RecommendationRunAccepted {
        if (runId <= 0 || !"PENDING".equals(status)) {
            throw new IllegalArgumentException("Accepted Recommendation Run is invalid");
        }
    }
}
