package com.informationplatform.hub.recommendation.api.dto;

import jakarta.validation.constraints.Positive;

/** View API 的可选 Recommendation Item 归因。 */
public record RecommendationInteractionAttributionRequest(
        /** 触发本次行为的 Recommendation Item 主键；不归因时可为空。 */
        @Positive Long recommendationItemId) {
}
