package com.informationplatform.hub.analysis.batch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchTerminalEvent;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/** 验证所有 Batch 终态发布统一事件，非终态不发布。 */
class AnalysisBatchTerminalEventPublisherTest {

    @Test
    void publishesCompletedPartialFailedFailedAndNoopOnly() {
        ApplicationEventPublisher springEvents = mock(ApplicationEventPublisher.class);
        AnalysisBatchTerminalEventPublisher publisher =
                new AnalysisBatchTerminalEventPublisher(springEvents);

        for (String status : List.of("COMPLETED", "PARTIAL_FAILED", "FAILED", "NOOP")) {
            publisher.publishIfTerminal(batch(status));
        }

        ArgumentCaptor<AnalysisBatchTerminalEvent> events =
                ArgumentCaptor.forClass(AnalysisBatchTerminalEvent.class);
        verify(springEvents, org.mockito.Mockito.times(4)).publishEvent(events.capture());
        assertThat(events.getAllValues())
                .extracting(AnalysisBatchTerminalEvent::status)
                .containsExactly("COMPLETED", "PARTIAL_FAILED", "FAILED", "NOOP");

        ApplicationEventPublisher nonTerminalEvents = mock(ApplicationEventPublisher.class);
        AnalysisBatchTerminalEventPublisher nonTerminalPublisher =
                new AnalysisBatchTerminalEventPublisher(nonTerminalEvents);
        nonTerminalPublisher.publishIfTerminal(batch("PENDING"));
        nonTerminalPublisher.publishIfTerminal(batch("RUNNING"));
        verify(nonTerminalEvents, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private AiAnalysisBatchPo batch(String status) {
        AiAnalysisBatchPo batch = new AiAnalysisBatchPo();
        batch.setId(31L);
        batch.setStatus(status);
        return batch;
    }
}
