package com.informationplatform.hub.recommendation.job.ranking.domain;

import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoredCandidate;
import java.util.List;

/** 冻结一次 JOB Recommendation V1 排序所需的已评分候选与结果上限。 */
public record JobRecommendationRankingRequest(
        /** TASK-038 产生的已评分候选；输入顺序不作为最终排序依据。 */
        List<JobRecommendationScoredCandidate> scoredCandidates,
        /** 最终推荐结果上限，范围 1..100。 */
        int topN) {

    public JobRecommendationRankingRequest {
        if (scoredCandidates == null
                || scoredCandidates.stream().anyMatch(candidate -> candidate == null)
                || topN < 1
                || topN > 100) {
            throw new IllegalArgumentException("JOB Recommendation ranking request is invalid");
        }
        scoredCandidates = List.copyOf(scoredCandidates);
    }
}
