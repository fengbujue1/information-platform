package com.informationplatform.hub.recommendation.api.dto;

import com.informationplatform.hub.recommendation.run.domain.RecommendationRunAccepted;

/** Manual Refresh 接受响应。 */
public record ManualRecommendationRefreshResponse(
        /** 新建 Recommendation Run 主键。 */ long runId,
        /** 初始状态，固定为 PENDING。 */ String status) {

    public static ManualRecommendationRefreshResponse from(RecommendationRunAccepted accepted) {
        return new ManualRecommendationRefreshResponse(accepted.runId(), accepted.status());
    }
}
