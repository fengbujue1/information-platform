package com.informationplatform.hub.recommendation.job.scoring.domain;

import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidate;

/** 保留 Candidate 来源事实并附加确定性评分的 TASK-038 输出。 */
public record JobRecommendationScoredCandidate(
        /** TASK-037 解析出的不可变来源候选。 */
        JobRecommendationCandidate candidate,
        /** JOB_RECOMMENDATION V1 评分结果。 */
        JobRecommendationScore score) {

    public JobRecommendationScoredCandidate {
        if (candidate == null || score == null) {
            throw new IllegalArgumentException("JOB scored candidate is invalid");
        }
    }
}
