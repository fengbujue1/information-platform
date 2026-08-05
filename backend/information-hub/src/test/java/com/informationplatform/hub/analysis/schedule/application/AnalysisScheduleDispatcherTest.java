package com.informationplatform.hub.analysis.schedule.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleDispatchResult;
import com.informationplatform.hub.analysis.schedule.infrastructure.config.AnalysisScheduleProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AnalysisScheduleDispatcherTest {

    @Test
    void processesDueSchedulesUntilTransactionReportsIdle() {
        AnalysisScheduleTransactionService transactions =
                mock(AnalysisScheduleTransactionService.class);
        Instant now = Instant.parse("2026-08-05T12:00:00Z");
        when(transactions.dispatchNext(now))
                .thenReturn(new AnalysisScheduleDispatchResult(
                        true, 31L, 51L, "CREATED"))
                .thenReturn(AnalysisScheduleDispatchResult.idle());
        AnalysisScheduleProperties properties = new AnalysisScheduleProperties();
        properties.setMaxSchedulesPerPoll(10);
        AnalysisScheduleDispatcher dispatcher = new AnalysisScheduleDispatcher(
                transactions, properties, Clock.fixed(now, ZoneOffset.UTC));

        dispatcher.poll();

        verify(transactions, times(2)).dispatchNext(now);
    }
}
