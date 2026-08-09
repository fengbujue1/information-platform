package com.informationplatform.hub.recommendation.application;

import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfile;

/** 保存后的组合 Profile 及本次是否新建。 */
public record RecommendationProfileChange(
        /** 保存后的完整 JOB Profile。 */ JobRecommendationProfile profile,
        /** true 表示首次创建，false 表示完整替换已有 Profile。 */ boolean created) {
}
