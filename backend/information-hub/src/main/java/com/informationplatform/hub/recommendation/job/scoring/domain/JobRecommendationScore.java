package com.informationplatform.hub.recommendation.job.scoring.domain;

import java.math.BigDecimal;
import java.util.List;

/** 一个 JOB Candidate 的确定性算法身份、最终分数与可解释结果。 */
public record JobRecommendationScore(
        /** 算法稳定 Key，V1 固定为 JOB_RECOMMENDATION。 */
        String algorithmKey,
        /** 算法版本，V1 固定为 1。 */
        int algorithmVersion,
        /** 加权最终分数，范围 0..100，保留三位小数。 */
        BigDecimal finalScore,
        /** 三个原始评分分项。 */
        JobRecommendationScoreBreakdown scoreBreakdown,
        /** 按稳定规则生成的面向用户推荐原因。 */
        List<String> reasons) {

    public JobRecommendationScore {
        if (algorithmKey == null
                || algorithmKey.isBlank()
                || algorithmVersion <= 0
                || finalScore == null
                || finalScore.scale() != 3
                || finalScore.compareTo(BigDecimal.ZERO) < 0
                || finalScore.compareTo(BigDecimal.valueOf(100)) > 0
                || scoreBreakdown == null
                || reasons == null
                || reasons.isEmpty()
                || reasons.stream().anyMatch(reason -> reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("JOB Recommendation score is invalid");
        }
        reasons = List.copyOf(reasons);
    }
}
