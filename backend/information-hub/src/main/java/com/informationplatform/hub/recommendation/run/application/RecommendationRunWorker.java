package com.informationplatform.hub.recommendation.run.application;

import com.informationplatform.hub.common.logging.OperationalLogExceptions;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunCompletion;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunExecutionResult;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunWork;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 只消费既有 PENDING Run 的模块化单体 Recommendation Worker。 */
@Component
@ConditionalOnProperty(
        prefix = "information-hub.recommendation",
        name = "worker-enabled",
        havingValue = "true")
public class RecommendationRunWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationRunWorker.class);
    private static final String EXECUTION_FAILED = "RECOMMENDATION_RUN_EXECUTION_FAILED";

    /** Worker 领取、恢复和完成的短事务。 */
    private final RecommendationRunWorkerTransactionService transactions;
    /** 事务外本地 Candidate/Scoring/Ranking 引擎。 */
    private final RecommendationRunExecutionService executionService;
    /** RUNNING 超过该阈值才允许恢复，避免处理仍活跃的执行。 */
    private final Duration staleAfter;

    public RecommendationRunWorker(
            RecommendationRunWorkerTransactionService transactions,
            RecommendationRunExecutionService executionService,
            @Value("${information-hub.recommendation.worker-stale-after:5m}")
                    Duration staleAfter) {
        this.transactions = transactions;
        this.executionService = executionService;
        this.staleAfter = staleAfter;
    }

    /** fixedDelay 串行执行一个 Run；轮询仅消费 PENDING，不创建新的业务刷新。 */
    @Scheduled(
            initialDelayString = "${information-hub.recommendation.worker-initial-delay:3s}",
            fixedDelayString = "${information-hub.recommendation.worker-fixed-delay:1s}")
    public void poll() {
        RecommendationRunWork work = null;
        long startedNanos = System.nanoTime();
        try {
            int recovered = transactions.recoverStale(staleAfter);
            if (recovered > 0) {
                LOGGER.warn("Recovered {} stale Recommendation Runs", recovered);
            }
            work = transactions.claimNext();
            if (work == null) {
                return;
            }
            LOGGER.info(
                    "Recommendation run started, recommendationRunId={}, informationType={}",
                    work.runId(),
                    work.informationType());
            RecommendationRunExecutionResult result = executionService.execute(work);
            RecommendationRunCompletion completion = transactions.complete(work.runId(), result);
            LOGGER.info(
                    "Recommendation run completed, recommendationRunId={}, status={}, candidateCount={}, eligibleCount={}, resultCount={}, durationMs={}",
                    work.runId(),
                    completion.status(),
                    result.candidateCount(),
                    result.eligibleCount(),
                    completion.resultCount(),
                    Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000));
        } catch (RuntimeException exception) {
            if (work != null) {
                try {
                    transactions.fail(work.runId(), EXECUTION_FAILED);
                } catch (RuntimeException failureException) {
                    LOGGER.error(
                            "Recommendation run failure completion failed, recommendationRunId={}",
                            work.runId(),
                            OperationalLogExceptions.sanitized(failureException));
                }
            }
            LOGGER.error(
                    "Recommendation run failed, recommendationRunId={}, failureCode={}, durationMs={}",
                    work == null ? null : work.runId(),
                    EXECUTION_FAILED,
                    Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000),
                    OperationalLogExceptions.sanitized(exception));
        }
    }
}
