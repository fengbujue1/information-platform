package com.informationplatform.hub.analysis.worker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchItemMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiModelInvocationMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.InformationAnalysisMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchItemPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiModelInvocationPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.InformationAnalysisPo;
import com.informationplatform.hub.analysis.processing.application.InformationAnalysisTransactionService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisBatchWorkerTransactionServiceTest {

    @Test
    void completesSuccessfulItemAndBatch() {
        Fixture fixture = fixture();
        AiAnalysisBatchItemPo item = runningItem();
        AiAnalysisBatchPo batch = runningBatch();
        when(fixture.items.selectByIdForUpdate(32)).thenReturn(item);
        when(fixture.batches.selectByIdForUpdate(31)).thenReturn(batch);
        when(fixture.items.selectList(any())).thenReturn(List.of(item));
        when(fixture.items.updateById(item)).thenReturn(1);
        when(fixture.batches.updateById(batch)).thenReturn(1);

        fixture.service.complete(32, 41, "SUCCEEDED");

        assertThat(item.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(batch.getStatus()).isEqualTo("COMPLETED");
        assertThat(batch.getCompletedAt()).isNotNull();
    }

    @Test
    void derivesPartialFailedWhenSuccessAndFailureCoexist() {
        Fixture fixture = fixture();
        AiAnalysisBatchItemPo failed = runningItem();
        AiAnalysisBatchItemPo succeeded = runningItem();
        succeeded.setId(33L);
        succeeded.setAnalysisId(42L);
        succeeded.setStatus("SUCCEEDED");
        AiAnalysisBatchPo batch = runningBatch();
        when(fixture.items.selectByIdForUpdate(32)).thenReturn(failed);
        when(fixture.batches.selectByIdForUpdate(31)).thenReturn(batch);
        when(fixture.items.selectList(any())).thenReturn(List.of(succeeded, failed));
        when(fixture.items.updateById(failed)).thenReturn(1);
        when(fixture.batches.updateById(batch)).thenReturn(1);

        fixture.service.complete(32, 41, "FAILED");

        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(batch.getStatus()).isEqualTo("PARTIAL_FAILED");
    }

    @Test
    void restartMarksInterruptedInvocationUnknownWithoutRetry() {
        Fixture fixture = fixture();
        AiAnalysisBatchItemPo item = runningItem();
        AiAnalysisBatchPo batch = runningBatch();
        AiModelInvocationPo invocation = new AiModelInvocationPo();
        invocation.setId(51L);
        invocation.setStatus("RUNNING");
        InformationAnalysisPo analysis = new InformationAnalysisPo();
        analysis.setId(41L);
        analysis.setStatus("RUNNING");
        when(fixture.items.selectRunningForUpdate()).thenReturn(List.of(item));
        when(fixture.batches.selectByIdForUpdate(31)).thenReturn(batch);
        when(fixture.invocations.selectRunningByBatchItemForUpdate(32))
                .thenReturn(invocation);
        when(fixture.analyses.selectOwnedByIdForUpdate(41, 7)).thenReturn(analysis);
        when(fixture.items.selectList(any())).thenReturn(List.of(item));
        when(fixture.items.updateById(item)).thenReturn(1);
        when(fixture.invocations.updateById(invocation)).thenReturn(1);
        when(fixture.analyses.updateById(analysis)).thenReturn(1);
        when(fixture.batches.updateById(batch)).thenReturn(1);

        assertThat(fixture.service.recoverInterrupted()).isEqualTo(1);

        assertThat(invocation.getStatus()).isEqualTo("UNKNOWN");
        assertThat(analysis.getStatus()).isEqualTo("FAILED");
        assertThat(item.getStatus()).isEqualTo("FAILED");
        assertThat(batch.getStatus()).isEqualTo("FAILED");
        verify(fixture.invocations).updateById(invocation);
    }

    private Fixture fixture() {
        AiAnalysisBatchMapper batches = mock(AiAnalysisBatchMapper.class);
        AiAnalysisBatchItemMapper items = mock(AiAnalysisBatchItemMapper.class);
        AiModelInvocationMapper invocations = mock(AiModelInvocationMapper.class);
        InformationAnalysisMapper analyses = mock(InformationAnalysisMapper.class);
        InformationAnalysisTransactionService analysisTransactions =
                mock(InformationAnalysisTransactionService.class);
        return new Fixture(
                batches,
                items,
                invocations,
                analyses,
                new AnalysisBatchWorkerTransactionService(
                        batches, items, invocations, analyses, analysisTransactions));
    }

    private AiAnalysisBatchItemPo runningItem() {
        AiAnalysisBatchItemPo item = new AiAnalysisBatchItemPo();
        item.setId(32L);
        item.setBatchId(31L);
        item.setAnalysisId(41L);
        item.setStatus("RUNNING");
        return item;
    }

    private AiAnalysisBatchPo runningBatch() {
        AiAnalysisBatchPo batch = new AiAnalysisBatchPo();
        batch.setId(31L);
        batch.setUserId(7L);
        batch.setStatus("RUNNING");
        return batch;
    }

    /** 单元测试使用的 Mapper 和 Service 集合。 */
    private record Fixture(
            /** Batch Mapper。 */ AiAnalysisBatchMapper batches,
            /** Item Mapper。 */ AiAnalysisBatchItemMapper items,
            /** Invocation Mapper。 */ AiModelInvocationMapper invocations,
            /** Analysis Mapper。 */ InformationAnalysisMapper analyses,
            /** 被测事务服务。 */ AnalysisBatchWorkerTransactionService service) {
    }
}
