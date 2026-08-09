package com.informationplatform.hub.recommendation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** 完整替换当前 Feedback 的请求。 */
public record PutRecommendationFeedbackRequest(
        /** Feedback current state：NONE、INTERESTED 或 NOT_INTERESTED。 */
        @NotBlank String feedbackState,
        /** 触发本次行为的 Recommendation Item 主键；不归因时可为空。 */
        @Positive Long recommendationItemId) {
}
