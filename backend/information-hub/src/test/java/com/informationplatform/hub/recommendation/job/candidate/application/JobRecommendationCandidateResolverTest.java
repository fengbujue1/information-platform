package com.informationplatform.hub.recommendation.job.candidate.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.definition.application.AnalysisDefinitionRegistry;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionId;
import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;
import com.informationplatform.hub.analysis.definition.domain.AnalysisPurpose;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidateRequest;
import com.informationplatform.hub.recommendation.job.candidate.domain.JobRecommendationCandidateResolution;
import com.informationplatform.hub.recommendation.job.candidate.infrastructure.persistence.JobRecommendationCandidateQueryMapper;
import com.informationplatform.hub.recommendation.job.candidate.infrastructure.persistence.JobRecommendationCandidateRow;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证兼容 Definition 选择、最高成功版本与稳定去重。 */
class JobRecommendationCandidateResolverTest {

    private final JobRecommendationCandidateQueryMapper queryMapper =
            mock(JobRecommendationCandidateQueryMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void selectsLatestCompatibleSuccessfulAnalysisPerInformation() {
        AnalysisDefinitionRegistry registry = registry(
                definition(1, 1, 1),
                definition(2, 1, 1),
                definition(3, 2, 2));
        JobRecommendationCandidateResolver resolver =
                new JobRecommendationCandidateResolver(queryMapper, objectMapper, registry);
        JobRecommendationCandidateRequest request = request();
        when(queryMapper.countCandidates(request, List.of(1, 2))).thenReturn(2L);
        when(queryMapper.selectEligibleCandidates(request, List.of(1, 2)))
                .thenReturn(List.of(
                        row(20L, 202L, 2, request.windowStart().plusHours(2)),
                        row(20L, 201L, 1, request.windowStart().plusHours(2)),
                        row(10L, 101L, 1, request.windowStart().plusHours(1))));

        JobRecommendationCandidateResolution resolution = resolver.resolve(request);

        assertEquals(2L, resolution.candidateCount());
        assertEquals(2L, resolution.eligibleCount());
        assertThat(resolution.candidates())
                .extracting(candidate -> candidate.informationId())
                .containsExactly(20L, 10L);
        assertEquals(2, resolution.candidates().getFirst().analysisDefinitionVersion());
        assertEquals(202L, resolution.candidates().getFirst().analysisId());
    }

    @SuppressWarnings("unchecked")
    private AnalysisDefinition<?, ?> definition(
            int version, int systemPromptVersion, int outputSchemaVersion) {
        AnalysisDefinition<Object, Object> definition = mock(AnalysisDefinition.class);
        when(definition.id()).thenReturn(new AnalysisDefinitionId("JOB_USER_RELEVANCE", version));
        when(definition.informationType()).thenReturn(AnalysisInformationType.JOB);
        when(definition.analysisPurpose()).thenReturn(AnalysisPurpose.USER_RELEVANCE);
        when(definition.systemPromptVersion()).thenReturn(systemPromptVersion);
        when(definition.outputSchemaVersion()).thenReturn(outputSchemaVersion);
        return definition;
    }

    private AnalysisDefinitionRegistry registry(AnalysisDefinition<?, ?>... definitions) {
        return new AnalysisDefinitionRegistry(List.of(definitions));
    }

    private JobRecommendationCandidateRequest request() {
        return new JobRecommendationCandidateRequest(
                9L,
                12L,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 8, 0, 0),
                new JobRecommendationProfilePreferences(
                        List.of(), List.of(), List.of(), List.of(), null, List.of("纯销售")));
    }

    private JobRecommendationCandidateRow row(
            long informationId,
            long analysisId,
            int definitionVersion,
            LocalDateTime firstSeenTime) {
        JobRecommendationCandidateRow row = new JobRecommendationCandidateRow();
        row.setInformationId(informationId);
        row.setSnapshotId(informationId + 1000);
        row.setAnalysisId(analysisId);
        row.setAnalysisDefinitionVersion(definitionVersion);
        row.setFirstSeenTime(firstSeenTime);
        row.setTitle("Java 后端");
        row.setContent("Spring Boot");
        row.setStandardizedPayload("{\"information\":{},\"job\":{}}");
        row.setAnalysisResult("{\"relevanceScore\":80,\"summary\":\"match\"}");
        row.setAiRelevanceScore(80);
        return row;
    }
}
