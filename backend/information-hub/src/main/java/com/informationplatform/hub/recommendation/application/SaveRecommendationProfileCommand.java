package com.informationplatform.hub.recommendation.application;

import java.util.List;

/** 保存当前 Owner 完整组合 Recommendation Profile 的应用命令。 */
public record SaveRecommendationProfileCommand(
        /** 绑定的 AI Prompt Profile 主键。 */ long analysisPromptProfileId,
        /** 推荐候选回看窗口天数。 */ int windowDays,
        /** 每轮推荐最大结果数。 */ int topN,
        /** 原始目标岗位数组。 */ List<String> targetRoles,
        /** 原始偏好技能数组。 */ List<String> preferredSkills,
        /** 原始偏好城市数组。 */ List<String> preferredCities,
        /** 原始远程类型数组。 */ List<String> preferredRemoteTypes,
        /** 最低期望月薪，单位为人民币元。 */ Integer salaryMinMonthlyYuan,
        /** 原始排除关键词数组。 */ List<String> excludedKeywords) {
}
