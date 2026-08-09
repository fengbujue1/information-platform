package com.informationplatform.hub.recommendation.job.ranking.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankedCandidate;
import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankingRequest;
import com.informationplatform.hub.recommendation.job.ranking.domain.JobRecommendationRankingResult;
import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoredCandidate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 实施 JOB_RECOMMENDATION V1 的稳定排序、去重、基础多样性和 Top N。 */
@Component
public class JobRecommendationRanker {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRecommendationRanker.class);
    private static final int DIVERSITY_WINDOW_SIZE = 20;
    private static final int COMPANY_CAP = 3;
    private static final int TITLE_CAP = 5;
    private static final Comparator<RankableCandidate> STABLE_COMPARATOR = Comparator
            .comparing((RankableCandidate value) -> value.scoredCandidate().score().finalScore())
            .reversed()
            .thenComparing(
                    value -> value.scoredCandidate().score().scoreBreakdown().freshnessScore(),
                    Comparator.reverseOrder())
            .thenComparing(
                    value -> value.scoredCandidate().candidate().informationId(),
                    Comparator.reverseOrder());

    /**
     * 将已评分 Candidate 转换为确定性 Top N，先去重和应用前 20 多样性，再按原始排序补足。
     *
     * <p>该纯计算过程不访问数据库，不持久化 Item，也不承担 Recommendation Run 生命周期。
     */
    public JobRecommendationRankingResult rank(JobRecommendationRankingRequest request) {
        long startedAt = System.nanoTime();
        List<RankableCandidate> sortedCandidates = request.scoredCandidates().stream()
                .map(this::toRankableCandidate)
                .sorted(STABLE_COMPARATOR)
                .toList();

        // 已按稳定比较器排序，首次出现即为同一 duplicate group 的最高优先级候选。
        Map<String, RankableCandidate> uniqueByDuplicateGroup = new LinkedHashMap<>();
        sortedCandidates.forEach(candidate ->
                uniqueByDuplicateGroup.putIfAbsent(candidate.duplicateGroupKey(), candidate));
        List<RankableCandidate> deduplicated = List.copyOf(uniqueByDuplicateGroup.values());

        List<RankableCandidate> selected = selectWithDiversity(deduplicated, request.topN());
        List<JobRecommendationRankedCandidate> rankedCandidates = new ArrayList<>(selected.size());
        for (int index = 0; index < selected.size(); index++) {
            RankableCandidate candidate = selected.get(index);
            rankedCandidates.add(new JobRecommendationRankedCandidate(
                    candidate.scoredCandidate(), index + 1, candidate.duplicateGroupKey()));
        }

        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (sortedCandidates.isEmpty()) {
            LOGGER.warn(
                    "Recommendation ranking skipped, algorithmKey=JOB_RECOMMENDATION, algorithmVersion=1, scoredCount=0, topN={}, durationMs={}",
                    request.topN(),
                    durationMs);
        } else {
            LOGGER.info(
                    "Recommendation ranking completed, algorithmKey=JOB_RECOMMENDATION, algorithmVersion=1, scoredCount={}, deduplicatedCount={}, resultCount={}, topN={}, durationMs={}",
                    sortedCandidates.size(),
                    deduplicated.size(),
                    rankedCandidates.size(),
                    request.topN(),
                    durationMs);
        }
        return new JobRecommendationRankingResult(
                sortedCandidates.size(), deduplicated.size(), rankedCandidates);
    }

    /** 前 20 个结果应用 company/title caps；不足 Top N 时按稳定原始顺序执行 fill pass。 */
    private List<RankableCandidate> selectWithDiversity(
            List<RankableCandidate> candidates, int topN) {
        int diversityTarget = Math.min(Math.min(topN, DIVERSITY_WINDOW_SIZE), candidates.size());
        List<RankableCandidate> selected = new ArrayList<>(Math.min(topN, candidates.size()));
        Set<Long> selectedInformationIds = new HashSet<>();
        Map<String, Integer> companyCounts = new HashMap<>();
        Map<String, Integer> titleCounts = new HashMap<>();

        for (RankableCandidate candidate : candidates) {
            if (selected.size() >= diversityTarget) {
                break;
            }
            if (companyCounts.getOrDefault(candidate.companyGroupKey(), 0) >= COMPANY_CAP
                    || titleCounts.getOrDefault(candidate.titleGroupKey(), 0) >= TITLE_CAP) {
                continue;
            }
            addSelected(candidate, selected, selectedInformationIds);
            companyCounts.merge(candidate.companyGroupKey(), 1, Integer::sum);
            titleCounts.merge(candidate.titleGroupKey(), 1, Integer::sum);
        }

        // Diversity 不能导致结果不足；补充阶段仍保持 duplicate 去重后的稳定排序。
        for (RankableCandidate candidate : candidates) {
            if (selected.size() >= topN) {
                break;
            }
            addSelected(candidate, selected, selectedInformationIds);
        }
        return selected;
    }

    private void addSelected(
            RankableCandidate candidate,
            List<RankableCandidate> selected,
            Set<Long> selectedInformationIds) {
        long informationId = candidate.scoredCandidate().candidate().informationId();
        if (selectedInformationIds.add(informationId)) {
            selected.add(candidate);
        }
    }

    private RankableCandidate toRankableCandidate(JobRecommendationScoredCandidate candidate) {
        JsonNode job = requireJobFacts(candidate.candidate().standardizedPayload());
        String company = normalizedGroupText(optionalText(job, "companyName"));
        String title = normalizedGroupText(candidate.candidate().title());
        String city = normalizedGroupText(optionalText(job, "cityName"));
        long informationId = candidate.candidate().informationId();
        return new RankableCandidate(
                candidate,
                duplicateGroupKey(company, title, city),
                fallbackGroupKey("company", company, informationId),
                fallbackGroupKey("title", title, informationId));
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
            return "";
        }
        if (!value.isTextual()) {
            throw invalid("Candidate " + field + " must be a string or null");
        }
        return value.textValue();
    }

    private String normalizedGroupText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .strip()
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private String fallbackGroupKey(String dimension, String normalizedValue, long informationId) {
        return normalizedValue.isEmpty()
                ? dimension + ":missing:" + informationId
                : normalizedValue;
    }

    /** 使用长度前缀避免字段拼接歧义，并生成适配 CHAR(64) 的确定性 SHA-256。 */
    private String duplicateGroupKey(String company, String title, String city) {
        String canonical = component(company) + component(title) + component(city);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String component(String value) {
        return value.length() + ":" + value;
    }

    private RecommendationRankingException invalid(String message) {
        return new RecommendationRankingException(message);
    }

    private record RankableCandidate(
            JobRecommendationScoredCandidate scoredCandidate,
            String duplicateGroupKey,
            String companyGroupKey,
            String titleGroupKey) {}
}
