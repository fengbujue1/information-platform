package com.informationplatform.hub.recommendation.job.domain;

import com.informationplatform.hub.recommendation.domain.RecommendationProfileCore;

/** Generic Recommendation Profile Core 与 JOB Extension 的完整组合领域对象。 */
public record JobRecommendationProfile(
        /** 通用 Profile Core。 */ RecommendationProfileCore core,
        /** JOB 专属结构化偏好。 */ JobRecommendationProfilePreferences preferences) {
}
