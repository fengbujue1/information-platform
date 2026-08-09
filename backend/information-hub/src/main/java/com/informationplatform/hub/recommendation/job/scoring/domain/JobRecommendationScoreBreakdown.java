package com.informationplatform.hub.recommendation.job.scoring.domain;

import java.math.BigDecimal;

/** JOB Recommendation V1 的三个可解释原始分项分数。 */
public record JobRecommendationScoreBreakdown(
        /** Phase 3 USER_RELEVANCE 分数，范围 0..100，保留三位小数。 */
        BigDecimal aiRelevanceScore,
        /** 已配置 JOB Profile 维度的等权匹配分数，范围 0..100，保留三位小数。 */
        BigDecimal profileMatchScore,
        /** 候选在冻结窗口中的线性新鲜度分数，范围 0..100，保留三位小数。 */
        BigDecimal freshnessScore) {

    public JobRecommendationScoreBreakdown {
        requireScore(aiRelevanceScore);
        requireScore(profileMatchScore);
        requireScore(freshnessScore);
    }

    private static void requireScore(BigDecimal score) {
        if (score == null
                || score.scale() != 3
                || score.compareTo(BigDecimal.ZERO) < 0
                || score.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("JOB Recommendation score breakdown is invalid");
        }
    }
}
