package com.informationplatform.hub.recommendation.job.scoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.provider.application.AiProviderClient;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidate;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScore;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoredCandidate;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoringRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** 验证 JOB_RECOMMENDATION V1 权重、维度归一化、边界、解释与确定性。 */
class JobRecommendationScorerTest {

    private static final LocalDateTime WINDOW_START = LocalDateTime.of(2026, 8, 1, 0, 0);
    private static final LocalDateTime WINDOW_END = WINDOW_START.plusDays(10);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JobRecommendationScorer scorer = new JobRecommendationScorer();

    @Test
    void calculatesAcceptedWeightsAndStableExplainability() {
        JobRecommendationCandidate candidate = candidate(
                11L,
                80,
                WINDOW_START.plusDays(5),
                "高级 Java 后端工程师",
                jobFacts("上海", "REMOTE", 30_000, List.of("Java", "Spring Boot")));
        JobRecommendationProfilePreferences profile = profile(
                List.of("Java 后端"),
                List.of("Spring"),
                List.of("上海"),
                List.of("REMOTE"),
                25_000);

        JobRecommendationScore score = score(candidate, profile);

        assertThat(score.algorithmKey()).isEqualTo("JOB_RECOMMENDATION");
        assertThat(score.algorithmVersion()).isEqualTo(1);
        assertThat(score.scoreBreakdown().aiRelevanceScore()).isEqualByComparingTo("80.000");
        assertThat(score.scoreBreakdown().profileMatchScore()).isEqualByComparingTo("100.000");
        assertThat(score.scoreBreakdown().freshnessScore()).isEqualByComparingTo("50.000");
        assertThat(score.finalScore()).isEqualByComparingTo("81.000");
        assertThat(score.reasons()).containsExactly(
                "AI 相关度高",
                "符合目标岗位偏好",
                "符合技能偏好",
                "符合城市偏好",
                "支持偏好办公方式",
                "满足最低薪资偏好");
    }

    @Test
    void normalizesOnlyConfiguredProfileDimensions() {
        JobRecommendationCandidate candidate = candidate(
                12L,
                50,
                WINDOW_START,
                "Java 后端",
                jobFacts("上海", "ONSITE", null, List.of("Go")));
        JobRecommendationProfilePreferences profile = profile(
                List.of("Java"),
                List.of("Spring"),
                List.of("上海"),
                List.of(),
                null);

        JobRecommendationScore score = score(candidate, profile);

        assertThat(score.scoreBreakdown().profileMatchScore()).isEqualByComparingTo("66.667");
        assertThat(score.reasons())
                .contains("符合目标岗位偏好", "符合城市偏好")
                .doesNotContain("符合技能偏好", "支持偏好办公方式", "满足最低薪资偏好");
    }

    @ParameterizedTest
    @MethodSource("individualDimensions")
    void scoresEachConfiguredProfileDimension(
            String title,
            JsonNode jobFacts,
            JobRecommendationProfilePreferences profile,
            String reason) {
        JobRecommendationScore score = score(
                candidate(20L, 60, WINDOW_START.plusDays(1), title, jobFacts),
                profile);

        assertThat(score.scoreBreakdown().profileMatchScore()).isEqualByComparingTo("100.000");
        assertThat(score.reasons()).contains(reason);
    }

    private static Stream<Arguments> individualDimensions() {
        ObjectMapper mapper = new ObjectMapper();
        return Stream.of(
                Arguments.of(
                        "JAVA　后端工程师",
                        jobFacts(mapper, null, null, null, List.of()),
                        profile(List.of("java后端"), List.of(), List.of(), List.of(), null),
                        "符合目标岗位偏好"),
                Arguments.of(
                        "岗位",
                        jobFacts(mapper, null, null, null, List.of("Spring Boot")),
                        profile(List.of(), List.of("spring"), List.of(), List.of(), null),
                        "符合技能偏好"),
                Arguments.of(
                        "岗位",
                        jobFacts(mapper, "成都", null, null, List.of()),
                        profile(List.of(), List.of(), List.of("成都"), List.of(), null),
                        "符合城市偏好"),
                Arguments.of(
                        "岗位",
                        jobFacts(mapper, null, "REMOTE", null, List.of()),
                        profile(List.of(), List.of(), List.of(), List.of("REMOTE"), null),
                        "支持偏好办公方式"),
                Arguments.of(
                        "岗位",
                        jobFacts(mapper, null, null, 20_000, List.of()),
                        profile(List.of(), List.of(), List.of(), List.of(), 20_000),
                        "满足最低薪资偏好"));
    }

    @Test
    void usesNeutralProfileScoreWhenNoDimensionIsConfigured() {
        JobRecommendationScore score = score(
                candidate(30L, 50, WINDOW_START, "岗位", jobFacts(null, null, null, List.of())),
                profile(List.of(), List.of(), List.of(), List.of(), null));

        assertThat(score.scoreBreakdown().profileMatchScore()).isEqualByComparingTo("100.000");
    }

    @Test
    void clampsFreshnessAndFinalScoresToAcceptedBounds() {
        JobRecommendationProfilePreferences mismatch = profile(
                List.of("不匹配"), List.of(), List.of(), List.of(), null);
        JobRecommendationScore minimum = score(
                candidate(40L, 0, WINDOW_START.minusDays(1), "岗位", jobFacts(null, null, null, List.of())),
                mismatch);
        JobRecommendationScore maximum = score(
                candidate(41L, 100, WINDOW_END.plusDays(1), "匹配岗位", jobFacts(null, null, null, List.of())),
                profile(List.of("匹配"), List.of(), List.of(), List.of(), null));

        assertThat(minimum.scoreBreakdown().freshnessScore()).isEqualByComparingTo("0.000");
        assertThat(minimum.finalScore()).isEqualByComparingTo("0.000");
        assertThat(maximum.scoreBreakdown().freshnessScore()).isEqualByComparingTo("100.000");
        assertThat(maximum.finalScore()).isEqualByComparingTo("100.000");
    }

    @Test
    void preservesInputOrderAndIsRepeatable() {
        JobRecommendationScoringRequest request = request(
                List.of(
                        candidate(51L, 70, WINDOW_START.plusDays(2), "A", jobFacts(null, null, null, List.of())),
                        candidate(52L, 90, WINDOW_START.plusDays(8), "B", jobFacts(null, null, null, List.of()))),
                profile(List.of(), List.of(), List.of(), List.of(), null));

        List<JobRecommendationScoredCandidate> first = scorer.scoreAll(request);
        List<JobRecommendationScoredCandidate> second = scorer.scoreAll(request);

        assertThat(first).isEqualTo(second);
        assertThat(first).extracting(result -> result.candidate().informationId())
                .containsExactly(51L, 52L);
    }

    @Test
    void rejectsMalformedStructuredFacts() throws Exception {
        JobRecommendationCandidate candidate = candidate(
                60L,
                80,
                WINDOW_START,
                "岗位",
                objectMapper.readTree("{\"cityName\":[]}"));

        assertThatThrownBy(() -> score(
                        candidate,
                        profile(List.of(), List.of(), List.of("上海"), List.of(), null)))
                .isInstanceOf(RecommendationScoringException.class)
                .hasMessageContaining("cityName");
    }

    @Test
    void hasNoAiProviderDependency() {
        boolean providerDependency = Arrays.stream(JobRecommendationScorer.class.getDeclaredConstructors())
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .anyMatch(AiProviderClient.class::isAssignableFrom);

        assertFalse(providerDependency);
    }

    private JobRecommendationScore score(
            JobRecommendationCandidate candidate,
            JobRecommendationProfilePreferences profile) {
        return scorer.scoreAll(request(List.of(candidate), profile)).getFirst().score();
    }

    private JobRecommendationScoringRequest request(
            List<JobRecommendationCandidate> candidates,
            JobRecommendationProfilePreferences profile) {
        return new JobRecommendationScoringRequest(
                candidates, profile, WINDOW_START, WINDOW_END);
    }

    private JobRecommendationCandidate candidate(
            long informationId,
            int aiScore,
            LocalDateTime firstSeen,
            String title,
            JsonNode jobFacts) {
        return new JobRecommendationCandidate(
                informationId,
                informationId + 100,
                informationId + 200,
                2,
                firstSeen,
                title,
                "正文",
                objectMapper.createObjectNode()
                        .set("job", jobFacts),
                objectMapper.createObjectNode().put("relevanceScore", aiScore),
                aiScore);
    }

    private JsonNode jobFacts(
            String city,
            String remoteType,
            Integer salaryMin,
            List<String> skills) {
        return jobFacts(objectMapper, city, remoteType, salaryMin, skills);
    }

    private static JsonNode jobFacts(
            ObjectMapper mapper,
            String city,
            String remoteType,
            Integer salaryMin,
            List<String> skills) {
        var node = mapper.createObjectNode();
        if (city == null) {
            node.putNull("cityName");
        } else {
            node.put("cityName", city);
        }
        if (remoteType == null) {
            node.putNull("remoteType");
        } else {
            node.put("remoteType", remoteType);
        }
        if (salaryMin == null) {
            node.putNull("salaryMinMonthlyYuan");
        } else {
            node.put("salaryMinMonthlyYuan", salaryMin);
        }
        var skillArray = node.putArray("sourceSkillTags");
        skills.forEach(skillArray::add);
        return node;
    }

    private static JobRecommendationProfilePreferences profile(
            List<String> targetRoles,
            List<String> preferredSkills,
            List<String> preferredCities,
            List<String> remoteTypes,
            Integer salaryMin) {
        return new JobRecommendationProfilePreferences(
                targetRoles,
                preferredSkills,
                preferredCities,
                remoteTypes,
                salaryMin,
                List.of());
    }
}
