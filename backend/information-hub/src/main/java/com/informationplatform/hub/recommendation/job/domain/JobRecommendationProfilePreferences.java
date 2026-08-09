package com.informationplatform.hub.recommendation.job.domain;

import java.util.List;

/** JOB Recommendation Profile 的规范化领域扩展偏好。 */
public record JobRecommendationProfilePreferences(
        /** 目标岗位名称，已去空、去重并稳定排序。 */ List<String> targetRoles,
        /** 偏好技能，已去空、去重并稳定排序。 */ List<String> preferredSkills,
        /** 偏好城市，已去空、去重并稳定排序。 */ List<String> preferredCities,
        /** ONSITE/HYBRID/REMOTE 偏好，已规范为大写并稳定排序。 */ List<String> preferredRemoteTypes,
        /** 最低期望月薪，单位为人民币元；未配置时为空。 */ Integer salaryMinMonthlyYuan,
        /** JOB Candidate hard exclusion 关键词，已去空、去重并稳定排序。 */ List<String> excludedKeywords) {
}
