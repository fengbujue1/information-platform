package com.informationplatform.hub.recommendation.job.ranking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidate;
import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankedCandidate;
import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankingRequest;
import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankingResult;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScore;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoreBreakdown;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoredCandidate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证 JOB_RECOMMENDATION V1 稳定排序、去重、基础多样性、补足与 Top N。 */
class JobRecommendationRankerTest {

    private static final LocalDateTime FIRST_SEEN = LocalDateTime.of(2026, 8, 1, 0, 0);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JobRecommendationRanker ranker = new JobRecommendationRanker();

    @Test
    void usesAcceptedStableComparatorForEqualScores() {
        List<JobRecommendationScoredCandidate> candidates = List.of(
                scored(11, "A", "公司A", "上海", "80.000", "60.000"),
                scored(12, "B", "公司B", "上海", "80.000", "90.000"),
                scored(13, "C", "公司C", "上海", "80.000", "90.000"),
                scored(14, "D", "公司D", "上海", "90.000", "10.000"));

        JobRecommendationRankingResult result = rank(candidates, 10);

        assertThat(informationIds(result)).containsExactly(14L, 13L, 12L, 11L);
        assertThat(result.rankedCandidates()).extracting(JobRecommendationRankedCandidate::rankNo)
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    void normalizesDuplicateKeyAndKeepsHighestRankedCandidate() {
        List<JobRecommendationScoredCandidate> candidates = List.of(
                scored(21, "Ｊａｖａ　工程师", " Example Co. ", " 上海 ", "70.000", "50.000"),
                scored(22, "java工程师", "example co.", "上海", "90.000", "50.000"),
                scored(23, "Go 工程师", "Example Co.", "上海", "80.000", "50.000"));

        JobRecommendationRankingResult result = rank(candidates, 10);

        assertThat(result.scoredCount()).isEqualTo(3);
        assertThat(result.deduplicatedCount()).isEqualTo(2);
        assertThat(informationIds(result)).containsExactly(22L, 23L);
        assertThat(result.rankedCandidates().getFirst().duplicateGroupKey())
                .matches("[0-9a-f]{64}");
    }

    @Test
    void appliesCompanyCapInDiversityPass() {
        List<JobRecommendationScoredCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            candidates.add(scored(
                    30 + index,
                    "岗位" + index,
                    "同一公司",
                    "城市" + index,
                    score(99 - index),
                    "50.000"));
        }
        candidates.add(scored(40, "其它岗位", "其它公司", "上海", "90.000", "50.000"));

        JobRecommendationRankingResult result = rank(candidates, 4);

        assertThat(informationIds(result)).containsExactly(30L, 31L, 32L, 40L);
    }

    @Test
    void appliesNormalizedTitleCapInDiversityPass() {
        List<JobRecommendationScoredCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            candidates.add(scored(
                    50 + index,
                    index % 2 == 0 ? "JAVA　工程师" : "java工程师",
                    "公司" + index,
                    "城市" + index,
                    score(99 - index),
                    "50.000"));
        }
        candidates.add(scored(60, "Go工程师", "其它公司", "上海", "90.000", "50.000"));

        JobRecommendationRankingResult result = rank(candidates, 6);

        assertThat(informationIds(result)).containsExactly(50L, 51L, 52L, 53L, 54L, 60L);
    }

    @Test
    void fillPassRestoresStableOrderWhenDiversityWouldUnderfill() {
        List<JobRecommendationScoredCandidate> candidates = List.of(
                scored(71, "岗位1", "同一公司", "城市1", "99.000", "50.000"),
                scored(72, "岗位2", "同一公司", "城市2", "98.000", "50.000"),
                scored(73, "岗位3", "同一公司", "城市3", "97.000", "50.000"),
                scored(74, "岗位4", "同一公司", "城市4", "96.000", "50.000"),
                scored(75, "岗位5", "同一公司", "城市5", "95.000", "50.000"));

        JobRecommendationRankingResult result = rank(candidates, 5);

        assertThat(informationIds(result)).containsExactly(71L, 72L, 73L, 74L, 75L);
    }

    @Test
    void limitsResultsToTopN() {
        List<JobRecommendationScoredCandidate> candidates = List.of(
                scored(81, "A", "公司A", "上海", "90.000", "50.000"),
                scored(82, "B", "公司B", "上海", "80.000", "50.000"),
                scored(83, "C", "公司C", "上海", "70.000", "50.000"));

        JobRecommendationRankingResult result = rank(candidates, 2);

        assertThat(informationIds(result)).containsExactly(81L, 82L);
        assertThat(result.rankedCandidates()).hasSize(2);
    }

    @Test
    void isDeterministicAcrossInputPermutationsAndRepeatedCalls() {
        List<JobRecommendationScoredCandidate> candidates = new ArrayList<>(List.of(
                scored(91, "A", "公司A", "上海", "80.000", "50.000"),
                scored(92, "B", "公司B", "上海", "90.000", "50.000"),
                scored(93, "C", "公司C", "上海", "70.000", "50.000")));
        JobRecommendationRankingResult expected = rank(candidates, 3);
        Collections.reverse(candidates);

        assertThat(rank(candidates, 3)).isEqualTo(expected);
        assertThat(rank(candidates, 3)).isEqualTo(rank(candidates, 3));
    }

    @Test
    void rejectsMalformedStructuredJobFacts() throws Exception {
        JobRecommendationScoredCandidate malformed = scored(
                101,
                "岗位",
                objectMapper.readTree("{\"companyName\":[],\"cityName\":\"上海\"}"),
                "80.000",
                "50.000");

        assertThatThrownBy(() -> rank(List.of(malformed), 1))
                .isInstanceOf(RecommendationRankingException.class)
                .hasMessageContaining("companyName");
    }

    private JobRecommendationRankingResult rank(
            List<JobRecommendationScoredCandidate> candidates, int topN) {
        return ranker.rank(new JobRecommendationRankingRequest(candidates, topN));
    }

    private List<Long> informationIds(JobRecommendationRankingResult result) {
        return result.rankedCandidates().stream()
                .map(value -> value.scoredCandidate().candidate().informationId())
                .toList();
    }

    private JobRecommendationScoredCandidate scored(
            long informationId,
            String title,
            String company,
            String city,
            String finalScore,
            String freshnessScore) {
        return scored(
                informationId,
                title,
                jobFacts(company, city),
                finalScore,
                freshnessScore);
    }

    private JobRecommendationScoredCandidate scored(
            long informationId,
            String title,
            JsonNode jobFacts,
            String finalScore,
            String freshnessScore) {
        JobRecommendationCandidate candidate = new JobRecommendationCandidate(
                informationId,
                informationId + 1_000,
                informationId + 2_000,
                2,
                FIRST_SEEN,
                title,
                "正文",
                objectMapper.createObjectNode().set("job", jobFacts),
                objectMapper.createObjectNode().put("relevanceScore", 80),
                80);
        JobRecommendationScore score = new JobRecommendationScore(
                "JOB_RECOMMENDATION",
                1,
                decimal(finalScore),
                new JobRecommendationScoreBreakdown(
                        decimal("80.000"), decimal("50.000"), decimal(freshnessScore)),
                List.of("AI 相关度高"));
        return new JobRecommendationScoredCandidate(candidate, score);
    }

    private JsonNode jobFacts(String company, String city) {
        return objectMapper.createObjectNode()
                .put("companyName", company)
                .put("cityName", city);
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private String score(int value) {
        return BigDecimal.valueOf(value).setScale(3).toPlainString();
    }
}
