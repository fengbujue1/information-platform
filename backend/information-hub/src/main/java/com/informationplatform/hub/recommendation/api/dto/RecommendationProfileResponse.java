package com.informationplatform.hub.recommendation.api.dto;

import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfile;
import java.time.LocalDateTime;
import java.util.List;

/** 对外隐藏 Core/Extension 拆表的完整 JOB Recommendation Profile 响应。 */
public record RecommendationProfileResponse(
        /** Recommendation Profile Core 主键。 */ long id,
        /** Information Type；Phase 4 当前为 JOB。 */ String informationType,
        /** 绑定的 AI Prompt Profile 主键。 */ long analysisPromptProfileId,
        /** 推荐候选回看窗口天数。 */ int windowDays,
        /** 每轮推荐最大结果数。 */ int topN,
        /** 规范化目标岗位数组。 */ List<String> targetRoles,
        /** 规范化偏好技能数组。 */ List<String> preferredSkills,
        /** 规范化偏好城市数组。 */ List<String> preferredCities,
        /** 规范化远程类型数组。 */ List<String> preferredRemoteTypes,
        /** 最低期望月薪，单位为人民币元。 */ Integer salaryMinMonthlyYuan,
        /** 规范化排除关键词数组。 */ List<String> excludedKeywords,
        /** 完整组合 Profile canonical SHA-256。 */ String contentHash,
        /** Profile 创建时间，按 UTC 返回。 */ LocalDateTime createdAt,
        /** Profile 最近更新时间，按 UTC 返回。 */ LocalDateTime updatedAt) {

    /** 将 Core + JOB Extension 组合领域对象展平为 Contract DTO。 */
    public static RecommendationProfileResponse from(JobRecommendationProfile profile) {
        return new RecommendationProfileResponse(
                profile.core().id(),
                profile.core().informationType(),
                profile.core().analysisPromptProfileId(),
                profile.core().windowDays(),
                profile.core().topN(),
                profile.preferences().targetRoles(),
                profile.preferences().preferredSkills(),
                profile.preferences().preferredCities(),
                profile.preferences().preferredRemoteTypes(),
                profile.preferences().salaryMinMonthlyYuan(),
                profile.preferences().excludedKeywords(),
                profile.core().contentHash(),
                profile.core().createdAt(),
                profile.core().updatedAt());
    }
}
