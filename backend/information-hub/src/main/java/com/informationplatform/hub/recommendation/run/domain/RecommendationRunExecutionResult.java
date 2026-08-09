package com.informationplatform.hub.recommendation.run.domain;

import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankedCandidate;
import java.util.List;

/** 事务外 Candidate/Scoring/Ranking 完成后的纯本地计算结果。 */
public record RecommendationRunExecutionResult(
        /** current usable + Analysis 条件命中的初始候选数。 */ long candidateCount,
        /** hard exclusion 后的候选数。 */ long eligibleCount,
        /** 去重、多样性与 Top N 后的最终排名结果。 */
        List<JobRecommendationRankedCandidate> rankedCandidates) {

    public RecommendationRunExecutionResult {
        if (candidateCount < 0
                || candidateCount > Integer.MAX_VALUE
                || eligibleCount < 0
                || eligibleCount > candidateCount
                || eligibleCount > Integer.MAX_VALUE
                || rankedCandidates == null
                || rankedCandidates.size() > eligibleCount) {
            throw new IllegalArgumentException("Recommendation Run execution result is invalid");
        }
        rankedCandidates = List.copyOf(rankedCandidates);
    }
}
