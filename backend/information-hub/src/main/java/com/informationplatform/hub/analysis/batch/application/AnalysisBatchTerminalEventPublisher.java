package com.informationplatform.hub.analysis.batch.application;

import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchTerminalEvent;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** 在 Batch 终态写入事务内发布进程内事件，由 AFTER_COMMIT 消费者解耦处理。 */
@Component
public class AnalysisBatchTerminalEventPublisher {

    private static final Set<String> TERMINAL_STATUSES =
            Set.of("COMPLETED", "PARTIAL_FAILED", "FAILED", "NOOP");

    /** Spring 进程内事件发布器。 */
    private final ApplicationEventPublisher eventPublisher;

    public AnalysisBatchTerminalEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** 仅为已持久化终态发布事件；PENDING/RUNNING 不产生 Recommendation 触发评估。 */
    public void publishIfTerminal(AiAnalysisBatchPo batch) {
        if (batch != null
                && batch.getId() != null
                && TERMINAL_STATUSES.contains(batch.getStatus())) {
            eventPublisher.publishEvent(
                    new AnalysisBatchTerminalEvent(batch.getId(), batch.getStatus()));
        }
    }
}
