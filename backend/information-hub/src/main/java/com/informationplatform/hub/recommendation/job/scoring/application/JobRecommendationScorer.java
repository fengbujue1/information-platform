package com.informationplatform.hub.recommendation.job.scoring.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidate;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScore;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoreBreakdown;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoredCandidate;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoringRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 实施 JOB_RECOMMENDATION V1 的无 Provider、确定性评分。 */
@Component
public class JobRecommendationScorer {

    public static final String ALGORITHM_KEY = "JOB_RECOMMENDATION";
    public static final int ALGORITHM_VERSION = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRecommendationScorer.class);
    private static final int SCORE_SCALE = 3;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal AI_WEIGHT = new BigDecimal("0.700");
    private static final BigDecimal PROFILE_WEIGHT = new BigDecimal("0.200");
    private static final BigDecimal FRESHNESS_WEIGHT = new BigDecimal("0.100");

    /**
     * 按输入顺序为全部 Candidate 评分，不承担排序、去重、多样性或持久化。
     *
     * <p>所有输入均来自 Run 冻结窗口、Profile 和当前 Snapshot；本方法不访问 Provider 或数据库。
     */
    public List<JobRecommendationScoredCandidate> scoreAll(
            JobRecommendationScoringRequest request) {
        long startedAt = System.nanoTime();
        List<JobRecommendationScoredCandidate> results = request.candidates().stream()
                .map(candidate -> new JobRecommendationScoredCandidate(
                        candidate,
                        score(candidate, request.profilePreferences(),
                                request.windowStart(), request.windowEnd())))
                .toList();
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (results.isEmpty()) {
            LOGGER.warn(
                    "Recommendation scoring skipped, algorithmKey={}, algorithmVersion={}, candidateCount=0, durationMs={}",
                    ALGORITHM_KEY,
                    ALGORITHM_VERSION,
                    durationMs);
        } else {
            LOGGER.info(
                    "Recommendation scoring completed, algorithmKey={}, algorithmVersion={}, candidateCount={}, durationMs={}",
                    ALGORITHM_KEY,
                    ALGORITHM_VERSION,
                    results.size(),
                    durationMs);
        }
        return results;
    }

    private JobRecommendationScore score(
            JobRecommendationCandidate candidate,
            JobRecommendationProfilePreferences profile,
            java.time.LocalDateTime windowStart,
            java.time.LocalDateTime windowEnd) {
        JsonNode job = requireJobFacts(candidate.standardizedPayload());
        List<String> reasons = new ArrayList<>();
        BigDecimal aiScore = scaled(candidate.aiRelevanceScore());
        reasons.add(aiReason(candidate.aiRelevanceScore()));

        ProfileScore profileScore = profileScore(candidate, job, profile);
        reasons.addAll(profileScore.reasons());
        BigDecimal freshnessScore = freshnessScore(candidate, windowStart, windowEnd);
        if (freshnessScore.compareTo(new BigDecimal("80.000")) >= 0) {
            reasons.add("最近进入平台");
        }

        BigDecimal finalScore = aiScore.multiply(AI_WEIGHT)
                .add(profileScore.score().multiply(PROFILE_WEIGHT))
                .add(freshnessScore.multiply(FRESHNESS_WEIGHT))
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
        return new JobRecommendationScore(
                ALGORITHM_KEY,
                ALGORITHM_VERSION,
                finalScore,
                new JobRecommendationScoreBreakdown(
                        aiScore, profileScore.score(), freshnessScore),
                reasons);
    }

    /** 配置过的五个 JOB Profile 维度等权；未配置维度不进入分母。 */
    private ProfileScore profileScore(
            JobRecommendationCandidate candidate,
            JsonNode job,
            JobRecommendationProfilePreferences profile) {
        int configuredDimensions = 0;
        int matchedDimensions = 0;
        List<String> reasons = new ArrayList<>();

        DimensionResult role = dimension(
                !profile.targetRoles().isEmpty(),
                () -> matchesAnyContained(profile.targetRoles(), candidate.title()),
                "符合目标岗位偏好");
        configuredDimensions += role.configuredCount();
        matchedDimensions += role.matchedCount();
        reasons.addAll(role.reasons());

        DimensionResult skill = dimension(
                !profile.preferredSkills().isEmpty(),
                () -> matchesAnySkill(profile.preferredSkills(), textArray(job, "sourceSkillTags")),
                "符合技能偏好");
        configuredDimensions += skill.configuredCount();
        matchedDimensions += skill.matchedCount();
        reasons.addAll(skill.reasons());

        DimensionResult city = dimension(
                !profile.preferredCities().isEmpty(),
                () -> matchesAnyExact(profile.preferredCities(), optionalText(job, "cityName")),
                "符合城市偏好");
        configuredDimensions += city.configuredCount();
        matchedDimensions += city.matchedCount();
        reasons.addAll(city.reasons());

        DimensionResult remote = dimension(
                !profile.preferredRemoteTypes().isEmpty(),
                () -> matchesAnyExact(profile.preferredRemoteTypes(), optionalText(job, "remoteType")),
                "支持偏好办公方式");
        configuredDimensions += remote.configuredCount();
        matchedDimensions += remote.matchedCount();
        reasons.addAll(remote.reasons());

        Integer requestedSalary = profile.salaryMinMonthlyYuan();
        DimensionResult salary = dimension(
                requestedSalary != null,
                () -> {
                    Integer candidateSalary = optionalNonNegativeInteger(job, "salaryMinMonthlyYuan");
                    return candidateSalary != null && candidateSalary >= requestedSalary;
                },
                "满足最低薪资偏好");
        configuredDimensions += salary.configuredCount();
        matchedDimensions += salary.matchedCount();
        reasons.addAll(salary.reasons());

        // 没有配置任何维度时使用中性满分，避免“未配置的维度”成为隐性负分。
        BigDecimal score = configuredDimensions == 0
                ? scaled(100)
                : BigDecimal.valueOf(matchedDimensions)
                        .multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(configuredDimensions), SCORE_SCALE,
                                RoundingMode.HALF_UP);
        return new ProfileScore(score, reasons);
    }

    private DimensionResult dimension(
            boolean configured, BooleanSupplier matcher, String matchedReason) {
        if (!configured) {
            return new DimensionResult(0, 0, List.of());
        }
        boolean matched = matcher.getAsBoolean();
        return new DimensionResult(
                1, matched ? 1 : 0, matched ? List.of(matchedReason) : List.of());
    }

    private BigDecimal freshnessScore(
            JobRecommendationCandidate candidate,
            java.time.LocalDateTime windowStart,
            java.time.LocalDateTime windowEnd) {
        long windowNanos = Duration.between(windowStart, windowEnd).toNanos();
        long elapsedNanos = Duration.between(windowStart, candidate.firstSeenTime()).toNanos();
        if (elapsedNanos <= 0) {
            return scaled(0);
        }
        if (elapsedNanos >= windowNanos) {
            return scaled(100);
        }
        return BigDecimal.valueOf(elapsedNanos)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(windowNanos), SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private JsonNode requireJobFacts(JsonNode standardizedPayload) {
        JsonNode job = standardizedPayload.get("job");
        if (job == null || !job.isObject()) {
            throw invalid("Candidate standardizedPayload.job must be an object");
        }
        return job;
    }

    private String optionalText(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw invalid("Candidate " + field + " must be a string or null");
        }
        return value.textValue();
    }

    private List<String> textArray(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            return List.of();
        }
        if (!value.isArray()) {
            throw invalid("Candidate " + field + " must be an array of strings or null");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode element : value) {
            if (!element.isTextual()) {
                throw invalid("Candidate " + field + " must contain only strings");
            }
            values.add(element.textValue());
        }
        return values;
    }

    private Integer optionalNonNegativeInteger(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0) {
            throw invalid("Candidate " + field + " must be a non-negative integer or null");
        }
        return value.intValue();
    }

    private boolean matchesAnyContained(List<String> expectedValues, String actualValue) {
        String normalizedActual = normalizedMatchText(actualValue);
        return !normalizedActual.isEmpty() && expectedValues.stream()
                .map(this::normalizedMatchText)
                .anyMatch(expected -> !expected.isEmpty() && normalizedActual.contains(expected));
    }

    private boolean matchesAnySkill(List<String> expectedValues, List<String> actualValues) {
        return expectedValues.stream().map(this::normalizedMatchText).anyMatch(expected ->
                !expected.isEmpty() && actualValues.stream().map(this::normalizedMatchText)
                        .anyMatch(actual -> !actual.isEmpty()
                                && (actual.contains(expected) || expected.contains(actual))));
    }

    private boolean matchesAnyExact(List<String> expectedValues, String actualValue) {
        String normalizedActual = normalizedMatchText(actualValue);
        return !normalizedActual.isEmpty() && expectedValues.stream()
                .map(this::normalizedMatchText)
                .anyMatch(normalizedActual::equals);
    }

    private String normalizedMatchText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .strip()
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private String aiReason(int aiScore) {
        if (aiScore >= 80) {
            return "AI 相关度高";
        }
        if (aiScore >= 60) {
            return "AI 相关度较高";
        }
        if (aiScore >= 40) {
            return "AI 相关度一般";
        }
        return "AI 相关度较低";
    }

    private BigDecimal scaled(int value) {
        return BigDecimal.valueOf(value).setScale(SCORE_SCALE, RoundingMode.UNNECESSARY);
    }

    private RecommendationScoringException invalid(String message) {
        return new RecommendationScoringException(message);
    }

    private record ProfileScore(BigDecimal score, List<String> reasons) {}

    private record DimensionResult(
            int configuredCount, int matchedCount, List<String> reasons) {}
}
