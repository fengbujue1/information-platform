package com.informationplatform.hub.recommendation.application;

import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/** 将 JOB 偏好规范为可稳定持久化和计算 contentHash 的领域值。 */
@Component
public class JobRecommendationProfileNormalizer {

    /** 每个结构化偏好数组最多保存的有效元素数。 */
    public static final int MAX_ITEMS_PER_LIST = 50;

    /** 单个偏好元素最大 Unicode 字符数。 */
    public static final int MAX_ITEM_CODE_POINTS = 100;

    /** Phase 4 JOB Profile 支持的远程类型。 */
    private static final Set<String> SUPPORTED_REMOTE_TYPES =
            Set.of("ONSITE", "HYBRID", "REMOTE");

    /** 去空、去重、排序并校验所有 JOB 扩展字段。 */
    public JobRecommendationProfilePreferences normalize(SaveRecommendationProfileCommand command) {
        List<String> targetRoles = normalizeList("targetRoles", command.targetRoles());
        List<String> preferredSkills = normalizeList("preferredSkills", command.preferredSkills());
        List<String> preferredCities = normalizeList("preferredCities", command.preferredCities());
        List<String> remoteTypes = normalizeRemoteTypes(command.preferredRemoteTypes());
        List<String> excludedKeywords =
                normalizeList("excludedKeywords", command.excludedKeywords());
        if (command.salaryMinMonthlyYuan() != null && command.salaryMinMonthlyYuan() < 0) {
            throw request(
                    "RECOMMENDATION_PROFILE_SALARY_INVALID",
                    "salaryMinMonthlyYuan must not be negative");
        }
        return new JobRecommendationProfilePreferences(
                targetRoles,
                preferredSkills,
                preferredCities,
                remoteTypes,
                command.salaryMinMonthlyYuan(),
                excludedKeywords);
    }

    private List<String> normalizeRemoteTypes(List<String> values) {
        List<String> normalized = normalizeList("preferredRemoteTypes", values).stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
        for (String value : normalized) {
            if (!SUPPORTED_REMOTE_TYPES.contains(value)) {
                throw request(
                        "RECOMMENDATION_PROFILE_REMOTE_TYPE_INVALID",
                        "preferredRemoteTypes only supports ONSITE, HYBRID or REMOTE");
            }
        }
        return normalized;
    }

    private List<String> normalizeList(String field, List<String> values) {
        TreeSet<String> normalized = new TreeSet<>();
        if (values != null) {
            for (String value : values) {
                String item = value == null ? "" : value.trim();
                if (item.isEmpty()) {
                    continue;
                }
                if (item.codePointCount(0, item.length()) > MAX_ITEM_CODE_POINTS) {
                    throw request(
                            "RECOMMENDATION_PROFILE_ITEM_TOO_LONG",
                            field + " items must not exceed 100 characters");
                }
                normalized.add(item);
            }
        }
        if (normalized.size() > MAX_ITEMS_PER_LIST) {
            throw request(
                    "RECOMMENDATION_PROFILE_LIST_TOO_LARGE",
                    field + " must not contain more than 50 items");
        }
        return List.copyOf(normalized);
    }

    private RecommendationProfileRequestException request(String code, String message) {
        return new RecommendationProfileRequestException(code, message);
    }
}
