package com.informationplatform.hub.recommendation.job.candidate.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.definition.application.AnalysisDefinitionRegistry;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;
import com.informationplatform.hub.analysis.definition.domain.AnalysisPurpose;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceDefinition;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidate;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidateRequest;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidateResolution;
import com.informationplatform.hub.recommendation.job.candidate.infrastructure.persistence.JobRecommendationCandidateQueryMapper;
import com.informationplatform.hub.recommendation.job.candidate.infrastructure.persistence.JobRecommendationCandidateRow;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 从当前 JOB Snapshot 与兼容成功 USER_RELEVANCE Analysis 解析候选。 */
@Component
public class JobRecommendationCandidateResolver {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JobRecommendationCandidateResolver.class);

    /** JOB Recommendation V1 消费的稳定 Analysis Definition Key。 */
    private static final String DEFINITION_KEY = JobUserRelevanceDefinition.KEY;

    /** 当前 Recommendation V1 可解释的 System Prompt 语义版本。 */
    private static final int COMPATIBLE_SYSTEM_PROMPT_VERSION = 1;

    /** 当前 Recommendation V1 可解释的输出 Schema 版本。 */
    private static final int COMPATIBLE_OUTPUT_SCHEMA_VERSION = 1;

    /** Candidate 只读查询 Mapper。 */
    private final JobRecommendationCandidateQueryMapper queryMapper;

    /** Snapshot 与 Analysis JSON 解析器。 */
    private final ObjectMapper objectMapper;

    /** 当前注册的兼容 JOB USER_RELEVANCE Definition 版本，按版本升序冻结。 */
    private final List<Integer> compatibleDefinitionVersions;

    public JobRecommendationCandidateResolver(
            JobRecommendationCandidateQueryMapper queryMapper,
            ObjectMapper objectMapper,
            AnalysisDefinitionRegistry definitionRegistry) {
        this.queryMapper = queryMapper;
        this.objectMapper = objectMapper;
        this.compatibleDefinitionVersions = resolveCompatibleVersions(definitionRegistry);
    }

    /**
     * 解析 frozen window 中的候选，并为每个 Information 选择最高兼容成功 Definition。
     *
     * <p>本方法只读，不创建 Analysis、Run 或 Item；数据库先应用 current usability、
     * Interaction 与关键词 hard exclusion，应用层按稳定 SQL 顺序消除兼容版本重复行。
     */
    @Transactional(readOnly = true)
    public JobRecommendationCandidateResolution resolve(
            JobRecommendationCandidateRequest request) {
        long startedAt = System.nanoTime();
        long candidateCount = queryMapper.countCandidates(
                request, compatibleDefinitionVersions);
        List<JobRecommendationCandidateRow> rows = queryMapper.selectEligibleCandidates(
                request, compatibleDefinitionVersions);

        // SQL 已按 Definition Version 倒序；首次出现即是该 Information 的最新兼容成功结果。
        Map<Long, JobRecommendationCandidateRow> latestByInformation = new LinkedHashMap<>();
        for (JobRecommendationCandidateRow row : rows) {
            if (row == null || row.getInformationId() == null) {
                throw persistence("Candidate query returned an invalid row");
            }
            latestByInformation.putIfAbsent(row.getInformationId(), row);
        }
        List<JobRecommendationCandidate> candidates = latestByInformation.values().stream()
                .map(this::toCandidate)
                .toList();
        if (candidateCount < candidates.size()) {
            throw persistence("Candidate query counts are inconsistent");
        }

        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (candidates.isEmpty()) {
            LOGGER.warn(
                    "Recommendation candidates empty, candidateCount={}, eligibleCount=0, durationMs={}",
                    candidateCount,
                    durationMs);
        } else {
            LOGGER.info(
                    "Recommendation candidates resolved, candidateCount={}, eligibleCount={}, durationMs={}",
                    candidateCount,
                    candidates.size(),
                    durationMs);
        }
        return new JobRecommendationCandidateResolution(
                candidateCount, candidates.size(), candidates);
    }

    private List<Integer> resolveCompatibleVersions(
            AnalysisDefinitionRegistry definitionRegistry) {
        List<Integer> versions = definitionRegistry.all().stream()
                .filter(this::isCompatible)
                .map(definition -> definition.id().version())
                .distinct()
                .sorted()
                .toList();
        if (versions.isEmpty()) {
            throw new IllegalStateException(
                    "No compatible JOB USER_RELEVANCE Definition is registered");
        }
        return versions;
    }

    private boolean isCompatible(AnalysisDefinition<?, ?> definition) {
        return DEFINITION_KEY.equals(definition.id().key())
                && definition.informationType() == AnalysisInformationType.JOB
                && definition.analysisPurpose() == AnalysisPurpose.USER_RELEVANCE
                && definition.systemPromptVersion() == COMPATIBLE_SYSTEM_PROMPT_VERSION
                && definition.outputSchemaVersion() == COMPATIBLE_OUTPUT_SCHEMA_VERSION;
    }

    private JobRecommendationCandidate toCandidate(JobRecommendationCandidateRow row) {
        if (row.getSnapshotId() == null
                || row.getAnalysisId() == null
                || row.getAnalysisDefinitionVersion() == null
                || !compatibleDefinitionVersions.contains(row.getAnalysisDefinitionVersion())
                || row.getFirstSeenTime() == null
                || row.getTitle() == null
                || row.getStandardizedPayload() == null
                || row.getAnalysisResult() == null
                || row.getAiRelevanceScore() == null) {
            throw persistence("Candidate query returned incomplete source facts");
        }
        try {
            JsonNode standardizedPayload =
                    objectMapper.readTree(row.getStandardizedPayload());
            JsonNode analysisResult = objectMapper.readTree(row.getAnalysisResult());
            return new JobRecommendationCandidate(
                    row.getInformationId(),
                    row.getSnapshotId(),
                    row.getAnalysisId(),
                    row.getAnalysisDefinitionVersion(),
                    row.getFirstSeenTime(),
                    row.getTitle(),
                    row.getContent(),
                    standardizedPayload,
                    analysisResult,
                    row.getAiRelevanceScore());
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            throw new RecommendationCandidatePersistenceException(
                    "Candidate JSON or source facts could not be read", exception);
        }
    }

    private RecommendationCandidatePersistenceException persistence(String message) {
        return new RecommendationCandidatePersistenceException(message);
    }
}
