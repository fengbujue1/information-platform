package com.informationplatform.hub.analysis.worker.application;

import com.informationplatform.hub.analysis.processing.application.InformationAnalysisService;
import com.informationplatform.hub.analysis.processing.domain.InformationAnalysisView;
import com.informationplatform.hub.analysis.worker.domain.AnalysisBatchWork;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 模块化单体内的单并发 MySQL 持久化 Batch Worker。 */
@Component
@ConditionalOnProperty(
        prefix = "information-hub.ai.batch",
        name = "worker-enabled",
        havingValue = "true")
public class AnalysisBatchWorker {

    /** 日志只记录 ID、数量和异常类型，不记录 Prompt、正文或秘密。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisBatchWorker.class);

    /** Worker 领取、完成和恢复短事务。 */
    private final AnalysisBatchWorkerTransactionService transactions;
    /** 事务外 Provider 执行和单条 Analysis 完成能力。 */
    private final InformationAnalysisService analysisService;
    /** 每个应用进程只执行一次遗留 RUNNING 恢复。 */
    private final AtomicBoolean recoveryCompleted = new AtomicBoolean();

    public AnalysisBatchWorker(
            AnalysisBatchWorkerTransactionService transactions,
            InformationAnalysisService analysisService) {
        this.transactions = transactions;
        this.analysisService = analysisService;
    }

    /**
     * 串行轮询并执行一个 Item。
     *
     * <p>fixedDelay 保证同一进程内不重叠；第一版明确不支持多实例分布式 Worker。
     */
    @Scheduled(
            initialDelayString = "${information-hub.ai.batch.worker-initial-delay:3s}",
            fixedDelayString = "${information-hub.ai.batch.worker-fixed-delay:1s}")
    public void poll() {
        AnalysisBatchWork work = null;
        try {
            if (!recoveryCompleted.get()) {
                int recovered = transactions.recoverInterrupted();
                recoveryCompleted.set(true);
                if (recovered > 0) {
                    LOGGER.warn("Recovered {} interrupted Analysis Batch items", recovered);
                }
            }
            work = transactions.claimNext();
            if (work == null) {
                return;
            }
            InformationAnalysisView result =
                    analysisService.executePrepared(work.preparation());
            transactions.complete(
                    work.batchItemId(),
                    work.preparation().executionPlan().analysisId(),
                    result.status());
        } catch (RuntimeException exception) {
            if (work != null) {
                try {
                    // 未分类执行异常已由单条能力持久化为 FAILED/UNKNOWN，Item 必须同步终结。
                    transactions.complete(
                            work.batchItemId(),
                            work.preparation().executionPlan().analysisId(),
                            "FAILED");
                } catch (RuntimeException completionException) {
                    LOGGER.error("Analysis Batch failure completion failed: {}",
                            completionException.getClass().getName());
                }
            }
            // Provider/持久化错误已在业务记录中脱敏；日志禁止输出请求或响应正文。
            LOGGER.error("Analysis Batch worker iteration failed: {}",
                    exception.getClass().getName());
        }
    }
}
