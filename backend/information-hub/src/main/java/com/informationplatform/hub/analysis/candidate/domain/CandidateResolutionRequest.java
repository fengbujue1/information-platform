package com.informationplatform.hub.analysis.candidate.domain;

import java.time.LocalDateTime;

/** 冻结身份与绝对窗口后的通用候选解析请求。 */
public record CandidateResolutionRequest(
        /** 当前 Owner 主键。 */
        long userId,
        /** 不可变 Prompt Version 主键。 */
        long promptVersionId,
        /** Analysis Definition Key。 */
        String definitionKey,
        /** Analysis Definition 版本。 */
        int definitionVersion,
        /** UTC 半开窗口起点。 */
        LocalDateTime windowStart,
        /** UTC 半开窗口终点。 */
        LocalDateTime windowEnd,
        /** 进入 Candidate Limit 的最大候选数。 */
        int maxCandidates) {

    public CandidateResolutionRequest {
        if (userId <= 0
                || promptVersionId <= 0
                || definitionKey == null
                || definitionKey.isBlank()
                || definitionVersion <= 0
                || windowStart == null
                || windowEnd == null
                || !windowStart.isBefore(windowEnd)
                || maxCandidates <= 0) {
            throw new IllegalArgumentException("Candidate resolution request is invalid");
        }
    }
}
