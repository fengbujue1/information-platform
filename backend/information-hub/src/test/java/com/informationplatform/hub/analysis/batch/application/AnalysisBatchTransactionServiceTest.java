package com.informationplatform.hub.analysis.batch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.candidate.domain.AnalysisCandidate;
import com.informationplatform.hub.analysis.batch.infrastructure.config.AnalysisBatchProperties;
import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;
import com.informationplatform.hub.analysis.definition.domain.AnalysisSnapshotSource;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchItemMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiModelInvocationMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchItemPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewService;
import com.informationplatform.hub.analysis.preview.domain.PreviewTokenPayload;
import com.informationplatform.hub.analysis.preview.domain.ResolvedAnalysisPreview;
import com.informationplatform.hub.analysis.preview.domain.ResolvedPreviewCandidate;
import com.informationplatform.hub.analysis.processing.domain.AnalysisTokenEstimate;
import com.informationplatform.hub.analysis.provider.application.AiProviderClient;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessage;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessageRole;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

class AnalysisBatchTransactionServiceTest {

    @Test
    void confirmUsesRepeatableReadTransaction() throws Exception {
        Transactional transactional = AnalysisBatchTransactionService.class
                .getMethod("createConfirmed", PreviewTokenPayload.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
    }

    @Test
    void freezesSelectedAndTokenBudgetDeferredItemsAtomically() {
        AiAnalysisBatchMapper batches = mock(AiAnalysisBatchMapper.class);
        AiAnalysisBatchItemMapper items = mock(AiAnalysisBatchItemMapper.class);
        AiModelInvocationMapper invocations = mock(AiModelInvocationMapper.class);
        AnalysisPreviewService previews = mock(AnalysisPreviewService.class);
        AiProviderClient provider = mock(AiProviderClient.class);
        AnalysisBatchProperties properties = new AnalysisBatchProperties();
        properties.setWorkerEnabled(true);
        PreviewTokenPayload payload = payload();
        ResolvedAnalysisPreview resolved = resolved();
        List<AiAnalysisBatchItemPo> insertedItems = new ArrayList<>();
        when(previews.recompute(payload)).thenReturn(resolved);
        when(batches.insert(any(AiAnalysisBatchPo.class))).thenAnswer(invocation -> {
            AiAnalysisBatchPo batch = invocation.getArgument(0);
            batch.setId(31L);
            return 1;
        });
        when(items.insert(any(AiAnalysisBatchItemPo.class))).thenAnswer(invocation -> {
            insertedItems.add(invocation.getArgument(0));
            return 1;
        });
        when(items.selectList(any())).thenReturn(List.of());

        AnalysisBatchTransactionService service = new AnalysisBatchTransactionService(
                batches, items, invocations, previews, provider, properties);
        service.createConfirmed(payload);

        ArgumentCaptor<AiAnalysisBatchPo> batchCaptor =
                ArgumentCaptor.forClass(AiAnalysisBatchPo.class);
        verify(batches).insert(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(batchCaptor.getValue().getSelectedCount()).isEqualTo(1);
        assertThat(insertedItems).extracting(AiAnalysisBatchItemPo::getStatus)
                .containsExactly("SELECTED", "DEFERRED");
        assertThat(insertedItems.get(1).getDecisionReason()).isEqualTo("TOKEN_BUDGET");
        verify(provider).validateRequest(resolved.candidates().get(0).providerRequest());
    }

    private PreviewTokenPayload payload() {
        Instant end = Instant.parse("2026-08-05T06:00:00Z");
        return new PreviewTokenPayload(
                1, 7, 11, 12, "JOB_USER_RELEVANCE", 1,
                end.minusSeconds(259_200), end, 3, 2, 1500,
                2, 2, 0, 1, 0, 1,
                100, 1000, 1100, "UTF8_BYTES_DIV3_MARGIN20_V1",
                "a".repeat(64), "request-1", end, end.plusSeconds(600));
    }

    private ResolvedAnalysisPreview resolved() {
        PreviewTokenPayload payload = payload();
        ResolvedPreviewCandidate selected = candidate(101, true);
        ResolvedPreviewCandidate deferred = candidate(102, false);
        return new ResolvedAnalysisPreview(
                7, 11, 12, "JOB", "JOB_USER_RELEVANCE", 1,
                payload.windowStart(), payload.windowEnd(), 3, 2, 1500,
                2, 2, 0, 1, 0, 1,
                100, 1000, 1100, "UTF8_BYTES_DIV3_MARGIN20_V1",
                "a".repeat(64), List.of(selected, deferred));
    }

    private ResolvedPreviewCandidate candidate(long id, boolean selected) {
        AnalysisSnapshotSource source = new AnalysisSnapshotSource(
                id + 1000,
                id,
                AnalysisInformationType.JOB,
                "title",
                "content",
                new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        AnalysisCandidate candidate = new AnalysisCandidate(
                id, id + 1000, LocalDateTime.of(2026, 8, 5, 1, 0), source);
        AiProviderRequest request = new AiProviderRequest(
                List.of(new AiProviderMessage(AiProviderMessageRole.USER, "candidate")), 1000);
        return new ResolvedPreviewCandidate(
                candidate,
                new AnalysisTokenEstimate(100, 1000, 1100,
                        "UTF8_BYTES_DIV3_MARGIN20_V1"),
                selected,
                request);
    }
}
