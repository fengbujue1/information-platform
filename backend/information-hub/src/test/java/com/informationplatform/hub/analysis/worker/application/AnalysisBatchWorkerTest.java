package com.informationplatform.hub.analysis.worker.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.processing.application.InformationAnalysisService;
import com.informationplatform.hub.analysis.processing.domain.AnalysisExecutionPlan;
import com.informationplatform.hub.analysis.processing.domain.AnalysisPreparation;
import com.informationplatform.hub.analysis.processing.domain.InformationAnalysisView;
import com.informationplatform.hub.analysis.worker.domain.AnalysisBatchWork;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class AnalysisBatchWorkerTest {

    @Test
    void recoversOnceThenExecutesPreparedWorkAndCompletesItem(CapturedOutput output) {
        AnalysisBatchWorkerTransactionService transactions =
                mock(AnalysisBatchWorkerTransactionService.class);
        InformationAnalysisService analyses = mock(InformationAnalysisService.class);
        AnalysisExecutionPlan plan = mock(AnalysisExecutionPlan.class);
        when(plan.analysisId()).thenReturn(41L);
        AnalysisPreparation preparation = AnalysisPreparation.execute(plan);
        AnalysisBatchWork work = new AnalysisBatchWork(31, 32, preparation);
        InformationAnalysisView result = mock(InformationAnalysisView.class);
        when(result.status()).thenReturn("SUCCEEDED");
        when(transactions.claimNext()).thenReturn(work).thenReturn(null);
        when(analyses.executePrepared(preparation)).thenReturn(result);
        AnalysisBatchWorker worker = new AnalysisBatchWorker(transactions, analyses);

        worker.poll();
        worker.poll();

        verify(transactions).recoverInterrupted();
        verify(analyses).executePrepared(preparation);
        verify(transactions).complete(32, 41, "SUCCEEDED");
        org.assertj.core.api.Assertions.assertThat(output).contains(
                "Analysis batch item started, batchId=31, batchItemId=32, analysisId=41",
                "Analysis batch item completed, batchId=31, batchItemId=32, analysisId=41, status=SUCCEEDED, durationMs=");
    }
}
