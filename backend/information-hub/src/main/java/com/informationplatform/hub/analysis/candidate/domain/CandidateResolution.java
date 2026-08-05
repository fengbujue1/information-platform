package com.informationplatform.hub.analysis.candidate.domain;

import java.util.List;

/** 窗口统计与进入 Candidate Limit 的有序待分析候选。 */
public record CandidateResolution(
        /** 窗口内具有当前 Snapshot 的 Information 总数。 */
        long totalInWindow,
        /** 相同逻辑身份已经成功分析的数量。 */
        long alreadyAnalyzedCount,
        /** 尚未成功分析的数量。 */
        long eligibleCount,
        /** 最多 maxCandidates 个稳定排序候选。 */
        List<AnalysisCandidate> candidates) {

    public CandidateResolution {
        if (totalInWindow < 0
                || alreadyAnalyzedCount < 0
                || eligibleCount != totalInWindow - alreadyAnalyzedCount
                || candidates == null
                || candidates.size() > eligibleCount) {
            throw new IllegalArgumentException("Candidate resolution is invalid");
        }
        candidates = List.copyOf(candidates);
    }
}
