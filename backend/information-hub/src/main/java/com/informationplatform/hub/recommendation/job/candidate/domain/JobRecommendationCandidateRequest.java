package com.informationplatform.hub.recommendation.job.candidate.domain;

import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import java.time.LocalDateTime;

/** 冻结一次 JOB Recommendation Candidate 解析所需的 Owner、Prompt、窗口与 Profile。 */
public record JobRecommendationCandidateRequest(
        /** Recommendation Owner 用户主键。 */
        long userId,
        /** Run 冻结的不可变 Prompt Version 主键。 */
        long promptVersionId,
        /** 候选窗口 UTC 左闭起点。 */
        LocalDateTime windowStart,
        /** 候选窗口 UTC 右开终点。 */
        LocalDateTime windowEnd,
        /** Run 冻结的规范化 JOB Profile 偏好。 */
        JobRecommendationProfilePreferences profilePreferences) {

    public JobRecommendationCandidateRequest {
        if (userId <= 0
                || promptVersionId <= 0
                || windowStart == null
                || windowEnd == null
                || !windowStart.isBefore(windowEnd)
                || profilePreferences == null
                || profilePreferences.excludedKeywords() == null
                || profilePreferences.excludedKeywords().stream()
                        .anyMatch(keyword -> keyword == null || keyword.isBlank())) {
            throw new IllegalArgumentException("JOB Recommendation Candidate request is invalid");
        }
    }
}
