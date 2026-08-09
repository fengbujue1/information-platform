package com.informationplatform.hub.recommendation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 计算 Generic Core + JOB Extension 完整 canonical representation 的 SHA-256。 */
@Component
public class JobRecommendationProfileHasher {

    /** 用于生成无敏感内容输出的稳定 canonical JSON。 */
    private final ObjectMapper objectMapper;

    public JobRecommendationProfileHasher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 固定字段顺序并使用已排序偏好数组计算稳定 hash。 */
    public String hash(
            String informationType,
            long analysisPromptProfileId,
            int windowDays,
            int topN,
            JobRecommendationProfilePreferences preferences) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("informationType", informationType);
        canonical.put("analysisPromptProfileId", analysisPromptProfileId);
        canonical.put("windowDays", windowDays);
        canonical.put("topN", topN);
        canonical.put("targetRoles", preferences.targetRoles());
        canonical.put("preferredSkills", preferences.preferredSkills());
        canonical.put("preferredCities", preferences.preferredCities());
        canonical.put("preferredRemoteTypes", preferences.preferredRemoteTypes());
        canonical.put("salaryMinMonthlyYuan", preferences.salaryMinMonthlyYuan());
        canonical.put("excludedKeywords", preferences.excludedKeywords());
        try {
            return hex(sha256().digest(objectMapper.writeValueAsBytes(canonical)));
        } catch (JsonProcessingException exception) {
            throw new RecommendationProfilePersistenceException(
                    "Recommendation Profile canonical JSON could not be encoded", exception);
        }
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String hex(byte[] bytes) {
        return java.util.HexFormat.of().formatHex(bytes);
    }
}
