package com.informationplatform.hub.recommendation.job.candidate.domain;

import java.util.List;

/** JOB Candidate 初始数量、hard exclusion 后数量与稳定候选列表。 */
public record JobRecommendationCandidateResolution(
        /** 当前可用、窗口内且存在兼容成功 Analysis 的初始候选数。 */
        long candidateCount,
        /** Interaction 与 excludedKeywords hard exclusion 后的候选数。 */
        long eligibleCount,
        /** 按 firstSeenTime、Information ID 稳定倒序的候选。 */
        List<JobRecommendationCandidate> candidates) {

    public JobRecommendationCandidateResolution {
        if (candidateCount < 0
                || eligibleCount < 0
                || eligibleCount > candidateCount
                || candidates == null
                || candidates.size() != eligibleCount) {
            throw new IllegalArgumentException("JOB Candidate resolution is invalid");
        }
        candidates = List.copyOf(candidates);
    }
}
