package com.informationplatform.hub.recommendation.job.run.domain;

import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import java.util.List;

/** Run 冻结的 Generic Profile Core 与 JOB Extension 完整快照 V1。 */
public record JobRecommendationRunProfileSnapshot(
        /** Profile snapshot schema 版本，当前固定为 1。 */ int schemaVersion,
        /** 冻结 Profile Core 主键。 */ long profileId,
        /** 冻结 Information Type，当前固定为 JOB。 */ String informationType,
        /** 冻结绑定的 Prompt Profile 主键。 */ long analysisPromptProfileId,
        /** 冻结候选回看窗口天数。 */ int windowDays,
        /** 冻结最终结果上限。 */ int topN,
        /** 冻结目标岗位偏好。 */ List<String> targetRoles,
        /** 冻结技能偏好。 */ List<String> preferredSkills,
        /** 冻结城市偏好。 */ List<String> preferredCities,
        /** 冻结办公方式偏好。 */ List<String> preferredRemoteTypes,
        /** 冻结最低月薪，人民币元；未配置为空。 */ Integer salaryMinMonthlyYuan,
        /** 冻结 hard exclusion 关键词。 */ List<String> excludedKeywords) {

    public JobRecommendationRunProfileSnapshot {
        if (schemaVersion != 1
                || profileId <= 0
                || !"JOB".equals(informationType)
                || analysisPromptProfileId <= 0
                || windowDays < 1
                || windowDays > 30
                || topN < 1
                || topN > 100
                || targetRoles == null
                || preferredSkills == null
                || preferredCities == null
                || preferredRemoteTypes == null
                || excludedKeywords == null
                || (salaryMinMonthlyYuan != null && salaryMinMonthlyYuan < 0)) {
            throw new IllegalArgumentException("JOB Recommendation Run Profile snapshot is invalid");
        }
        targetRoles = List.copyOf(targetRoles);
        preferredSkills = List.copyOf(preferredSkills);
        preferredCities = List.copyOf(preferredCities);
        preferredRemoteTypes = List.copyOf(preferredRemoteTypes);
        excludedKeywords = List.copyOf(excludedKeywords);
    }

    /** 将冻结 JOB 字段恢复为 Candidate/Scoring 共用的不可变偏好。 */
    public JobRecommendationProfilePreferences preferences() {
        return new JobRecommendationProfilePreferences(
                targetRoles,
                preferredSkills,
                preferredCities,
                preferredRemoteTypes,
                salaryMinMonthlyYuan,
                excludedKeywords);
    }
}
