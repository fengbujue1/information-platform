package com.informationplatform.hub.recommendation.job.scoring.domain;

import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidate;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import java.time.LocalDateTime;
import java.util.List;

/** 冻结一次 JOB Recommendation V1 评分所需的候选、Profile 与时间窗口。 */
public record JobRecommendationScoringRequest(
        /** 按 Candidate Resolver 稳定顺序提供的候选列表。 */
        List<JobRecommendationCandidate> candidates,
        /** Run 冻结的规范化 JOB Profile 偏好。 */
        JobRecommendationProfilePreferences profilePreferences,
        /** Freshness 归一化窗口的 UTC 左闭起点。 */
        LocalDateTime windowStart,
        /** Freshness 归一化窗口的 UTC 右开终点。 */
        LocalDateTime windowEnd) {

    public JobRecommendationScoringRequest {
        if (candidates == null
                || candidates.stream().anyMatch(candidate -> candidate == null)
                || profilePreferences == null
                || profilePreferences.targetRoles() == null
                || profilePreferences.preferredSkills() == null
                || profilePreferences.preferredCities() == null
                || profilePreferences.preferredRemoteTypes() == null
                || profilePreferences.excludedKeywords() == null
                || invalidValues(profilePreferences.targetRoles())
                || invalidValues(profilePreferences.preferredSkills())
                || invalidValues(profilePreferences.preferredCities())
                || invalidValues(profilePreferences.preferredRemoteTypes())
                || invalidValues(profilePreferences.excludedKeywords())
                || (profilePreferences.salaryMinMonthlyYuan() != null
                        && profilePreferences.salaryMinMonthlyYuan() < 0)
                || windowStart == null
                || windowEnd == null
                || !windowStart.isBefore(windowEnd)) {
            throw new IllegalArgumentException("JOB Recommendation scoring request is invalid");
        }
        candidates = List.copyOf(candidates);
    }

    private static boolean invalidValues(List<String> values) {
        return values.stream().anyMatch(value -> value == null || value.isBlank());
    }
}
