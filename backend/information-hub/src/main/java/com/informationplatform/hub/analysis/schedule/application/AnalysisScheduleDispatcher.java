package com.informationplatform.hub.analysis.schedule.application;

import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleDispatchResult;
import com.informationplatform.hub.analysis.schedule.infrastructure.config.AnalysisScheduleProperties;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 模块化单体内每 30 秒扫描 due Schedule 的低并发 Dispatcher。 */
@Component
@ConditionalOnProperty(
        prefix = "information-hub.ai.schedule",
        name = "dispatcher-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AnalysisScheduleDispatcher {

    /** 日志只记录 Schedule/Batch ID 和稳定结果，不记录 Prompt 或秘密。 */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AnalysisScheduleDispatcher.class);

    /** 每个 Schedule 独立事务的触发能力。 */
    private final AnalysisScheduleTransactionService transactions;
    /** 单轮上限等服务端参数。 */
    private final AnalysisScheduleProperties properties;
    /** 可替换 UTC 时钟。 */
    private final Clock clock;

    public AnalysisScheduleDispatcher(
            AnalysisScheduleTransactionService transactions,
            AnalysisScheduleProperties properties,
            Clock clock) {
        this.transactions = transactions;
        this.properties = properties;
        this.clock = clock;
    }

    /** 有界处理不同 due Schedule；每次事务最多锁定一行并原子推进。 */
    @Scheduled(
            initialDelayString =
                    "${information-hub.ai.schedule.dispatcher-initial-delay:5s}",
            fixedDelayString =
                    "${information-hub.ai.schedule.dispatcher-fixed-delay:30s}")
    public void poll() {
        int limit = Math.max(1, properties.getMaxSchedulesPerPoll());
        for (int index = 0; index < limit; index++) {
            try {
                AnalysisScheduleDispatchResult result =
                        transactions.dispatchNext(clock.instant());
                if (!result.processed()) {
                    return;
                }
                if (!"CREATED".equals(result.outcome())) {
                    LOGGER.info(
                            "Analysis Schedule dispatch outcome={} scheduleId={} batchId={}",
                            result.outcome(),
                            result.scheduleId(),
                            result.batchId());
                }
            } catch (RuntimeException exception) {
                // 单次失败留待下一轮重试；日志禁止输出 SQL 参数、Prompt 或 Provider 配置。
                LOGGER.error(
                        "Analysis Schedule dispatch failed: {}",
                        exception.getClass().getName());
                return;
            }
        }
    }
}
