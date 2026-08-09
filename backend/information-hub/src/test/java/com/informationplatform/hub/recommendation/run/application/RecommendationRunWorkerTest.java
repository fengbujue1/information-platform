package com.informationplatform.hub.recommendation.run.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.provider.application.AiProviderClient;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunCompletion;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunExecutionResult;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunWork;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** 验证 Worker 执行、失败终结、duration 日志和无 Provider 依赖。 */
@ExtendWith(OutputCaptureExtension.class)
class RecommendationRunWorkerTest {

    @Test
    void recoversClaimsExecutesAndCompletesWithDurationLog(CapturedOutput output) {
        RecommendationRunWorkerTransactionService transactions =
                mock(RecommendationRunWorkerTransactionService.class);
        RecommendationRunExecutionService execution = mock(RecommendationRunExecutionService.class);
        RecommendationRunWork work = work();
        RecommendationRunExecutionResult result =
                new RecommendationRunExecutionResult(0, 0, List.of());
        when(transactions.claimNext()).thenReturn(work);
        when(execution.execute(work)).thenReturn(result);
        when(transactions.complete(41, result))
                .thenReturn(new RecommendationRunCompletion("NOOP", 0));

        new RecommendationRunWorker(transactions, execution, Duration.ofMinutes(5)).poll();

        verify(transactions).recoverStale(Duration.ofMinutes(5));
        verify(execution).execute(work);
        verify(transactions).complete(41, result);
        assertThat(output).contains(
                "Recommendation run started, recommendationRunId=41",
                "Recommendation run completed, recommendationRunId=41",
                "durationMs=");
    }

    @Test
    void marksExecutionFailureAndHasNoProviderDependency(CapturedOutput output) {
        RecommendationRunWorkerTransactionService transactions =
                mock(RecommendationRunWorkerTransactionService.class);
        RecommendationRunExecutionService execution = mock(RecommendationRunExecutionService.class);
        RecommendationRunWork work = work();
        when(transactions.claimNext()).thenReturn(work);
        when(execution.execute(work)).thenThrow(new IllegalStateException("sensitive payload"));

        new RecommendationRunWorker(transactions, execution, Duration.ofMinutes(5)).poll();

        verify(transactions).fail(41, "RECOMMENDATION_RUN_EXECUTION_FAILED");
        assertThat(output.getOut())
                .contains("Recommendation run failed, recommendationRunId=41")
                .doesNotContain("sensitive payload");
        boolean providerDependency = Arrays.stream(
                        RecommendationRunExecutionService.class.getDeclaredConstructors())
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .anyMatch(AiProviderClient.class::isAssignableFrom);
        assertThat(providerDependency).isFalse();
    }

    private RecommendationRunWork work() {
        return new RecommendationRunWork(
                41,
                7,
                "JOB",
                21,
                "{\"schemaVersion\":1}",
                31,
                32,
                "JOB_RECOMMENDATION",
                1,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 8, 0, 0));
    }
}
