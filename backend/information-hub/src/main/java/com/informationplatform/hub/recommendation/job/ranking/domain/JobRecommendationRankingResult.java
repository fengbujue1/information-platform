package com.informationplatform.hub.recommendation.job.ranking.domain;

import java.util.List;

/** JOB Recommendation V1 去重前后数量和最终 Top N 排名结果。 */
public record JobRecommendationRankingResult(
        /** 进入 Ranker 的已评分候选数量。 */
        int scoredCount,
        /** duplicateGroupKey 去重后的候选数量。 */
        int deduplicatedCount,
        /** 应用 diversity、fill pass 与 Top N 后的连续排名结果。 */
        List<JobRecommendationRankedCandidate> rankedCandidates) {

    public JobRecommendationRankingResult {
        if (scoredCount < 0
                || deduplicatedCount < 0
                || deduplicatedCount > scoredCount
                || rankedCandidates == null
                || rankedCandidates.size() > deduplicatedCount) {
            throw new IllegalArgumentException("JOB Recommendation ranking result is invalid");
        }
        rankedCandidates = List.copyOf(rankedCandidates);
        for (int index = 0; index < rankedCandidates.size(); index++) {
            if (rankedCandidates.get(index).rankNo() != index + 1) {
                throw new IllegalArgumentException("JOB Recommendation ranks must be continuous");
            }
        }
    }
}
