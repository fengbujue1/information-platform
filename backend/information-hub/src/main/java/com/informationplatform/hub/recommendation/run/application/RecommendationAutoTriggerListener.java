package com.informationplatform.hub.recommendation.run.application;

import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchTerminalEvent;
import com.informationplatform.hub.common.logging.OperationalLogExceptions;
import com.informationplatform.hub.recommendation.run.domain.RecommendationAutoTriggerOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 在 Analysis Batch 事务提交后评估并创建幂等 Auto Recommendation Run。 */
@Component
public class RecommendationAutoTriggerListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RecommendationAutoTriggerListener.class);

    /** Recommendation Run 创建短事务。 */
    private final RecommendationRunCreationTransactionService runCreation;

    public RecommendationAutoTriggerListener(
            RecommendationRunCreationTransactionService runCreation) {
        this.runCreation = runCreation;
    }

    /**
     * AFTER_COMMIT 后独立评估 Batch；Recommendation 失败不得回滚或伪装已提交的 Analysis Batch。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTerminal(AnalysisBatchTerminalEvent event) {
        long startedNanos = System.nanoTime();
        LOGGER.info(
                "Recommendation trigger evaluating, sourceAnalysisBatchId={}, batchStatus={}",
                event.batchId(),
                event.status());
        try {
            RecommendationAutoTriggerOutcome outcome = runCreation.createAuto(event.batchId());
            if (outcome.created()) {
                LOGGER.info(
                        "Recommendation run created, recommendationRunId={}, triggerType=ANALYSIS_BATCH_COMPLETED, sourceAnalysisBatchId={}, durationMs={}",
                        outcome.runId(),
                        event.batchId(),
                        elapsedMillis(startedNanos));
            } else {
                LOGGER.warn(
                        "Recommendation trigger skipped, sourceAnalysisBatchId={}, reason={}, durationMs={}",
                        event.batchId(),
                        outcome.skipReason(),
                        elapsedMillis(startedNanos));
            }
        } catch (DuplicateKeyException exception) {
            // 数据库唯一键是并发重复事件的最终幂等防线。
            LOGGER.warn(
                    "Recommendation trigger skipped, sourceAnalysisBatchId={}, reason=SOURCE_BATCH_ALREADY_TRIGGERED, durationMs={}",
                    event.batchId(),
                    elapsedMillis(startedNanos));
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Recommendation trigger failed, sourceAnalysisBatchId={}, durationMs={}",
                    event.batchId(),
                    elapsedMillis(startedNanos),
                    OperationalLogExceptions.sanitized(exception));
        }
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }
}
