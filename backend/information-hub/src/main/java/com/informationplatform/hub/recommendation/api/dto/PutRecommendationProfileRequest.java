package com.informationplatform.hub.recommendation.api.dto;

import com.informationplatform.hub.recommendation.application.SaveRecommendationProfileCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/** 完整替换当前 Owner JOB Recommendation Profile 的请求。 */
public record PutRecommendationProfileRequest(
        /** 绑定的同 Owner AI Prompt Profile 主键。 */ @NotNull @Positive Long analysisPromptProfileId,
        /** 推荐候选回看窗口天数，范围 1 至 30。 */ @NotNull @Min(1) @Max(30) Integer windowDays,
        /** 每轮推荐最大结果数，范围 1 至 100。 */ @NotNull @Min(1) @Max(100) Integer topN,
        /** 目标岗位数组；空值按空数组处理。 */ List<String> targetRoles,
        /** 偏好技能数组；空值按空数组处理。 */ List<String> preferredSkills,
        /** 偏好城市数组；空值按空数组处理。 */ List<String> preferredCities,
        /** ONSITE/HYBRID/REMOTE 偏好数组。 */ List<String> preferredRemoteTypes,
        /** 最低期望月薪，单位为人民币元；未配置时为空。 */ @PositiveOrZero Integer salaryMinMonthlyYuan,
        /** JOB Candidate hard exclusion 关键词数组。 */ List<String> excludedKeywords) {

    /** 转为不携带 userId 的应用命令，Owner 仍由 Session 解析。 */
    public SaveRecommendationProfileCommand toCommand() {
        return new SaveRecommendationProfileCommand(
                analysisPromptProfileId,
                windowDays,
                topN,
                targetRoles,
                preferredSkills,
                preferredCities,
                preferredRemoteTypes,
                salaryMinMonthlyYuan,
                excludedKeywords);
    }
}
