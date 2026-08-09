package com.informationplatform.hub.recommendation.run.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchTerminalEvent;
import com.informationplatform.hub.recommendation.run.domain.RecommendationAutoTriggerOutcome;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 验证 AFTER_COMMIT Auto Trigger 编排、聚合日志与 Analysis Batch 解耦失败边界。 */
@ExtendWith(OutputCaptureExtension.class)
class RecommendationAutoTriggerListenerTest {

    @Test
    void listenerIsStrictlyAfterCommit() throws Exception {
        Method method = RecommendationAutoTriggerListener.class
                .getMethod("onTerminal", AnalysisBatchTerminalEvent.class);
        TransactionalEventListener annotation =
                method.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.fallbackExecution()).isFalse();
    }

    @Test
    void logsCreatedAndSkippedOutcomesWithDuration(CapturedOutput output) {
        RecommendationRunCreationTransactionService creation =
                mock(RecommendationRunCreationTransactionService.class);
        RecommendationAutoTriggerListener listener =
                new RecommendationAutoTriggerListener(creation);
        when(creation.createAuto(31L))
                .thenReturn(RecommendationAutoTriggerOutcome.created(41L));
        when(creation.createAuto(32L))
                .thenReturn(RecommendationAutoTriggerOutcome.skipped(
                        "RECOMMENDATION_PROFILE_NOT_FOUND"));

        listener.onTerminal(new AnalysisBatchTerminalEvent(31, "COMPLETED"));
        listener.onTerminal(new AnalysisBatchTerminalEvent(32, "PARTIAL_FAILED"));

        verify(creation).createAuto(31L);
        verify(creation).createAuto(32L);
        assertThat(output).contains(
                "Recommendation trigger evaluating, sourceAnalysisBatchId=31",
                "Recommendation run created, recommendationRunId=41",
                "Recommendation trigger skipped, sourceAnalysisBatchId=32, reason=RECOMMENDATION_PROFILE_NOT_FOUND",
                "durationMs=");
    }

    @Test
    void triggerFailureDoesNotEscapeOrLeakSensitiveMessage(CapturedOutput output) {
        RecommendationRunCreationTransactionService creation =
                mock(RecommendationRunCreationTransactionService.class);
        RecommendationAutoTriggerListener listener =
                new RecommendationAutoTriggerListener(creation);
        when(creation.createAuto(31L))
                .thenThrow(new IllegalStateException("sensitive payload"));

        listener.onTerminal(new AnalysisBatchTerminalEvent(31, "COMPLETED"));

        assertThat(output.getOut())
                .contains("Recommendation trigger failed, sourceAnalysisBatchId=31")
                .doesNotContain("sensitive payload");
    }
}
