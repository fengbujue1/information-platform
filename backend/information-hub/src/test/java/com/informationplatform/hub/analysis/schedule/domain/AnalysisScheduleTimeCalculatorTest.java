package com.informationplatform.hub.analysis.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AnalysisScheduleTimeCalculatorTest {

    private final AnalysisScheduleTimeCalculator calculator =
            new AnalysisScheduleTimeCalculator();

    @Test
    void calculatesNextWallClockTimeInUserZone() {
        Instant next = calculator.nextRunAfter(
                LocalTime.of(2, 0),
                ZoneId.of("Asia/Shanghai"),
                Instant.parse("2026-08-05T12:00:00Z"));

        assertThat(next).isEqualTo(Instant.parse("2026-08-05T18:00:00Z"));
    }

    @Test
    void movesDstGapToFirstValidTime() {
        Instant next = calculator.nextRunAfter(
                LocalTime.of(2, 30),
                ZoneId.of("America/New_York"),
                Instant.parse("2026-03-08T05:00:00Z"));

        assertThat(next).isEqualTo(Instant.parse("2026-03-08T07:00:00Z"));
    }

    @Test
    void choosesEarlierOffsetDuringDstOverlap() {
        Instant next = calculator.nextRunAfter(
                LocalTime.of(1, 30),
                ZoneId.of("America/New_York"),
                Instant.parse("2026-11-01T04:00:00Z"));

        assertThat(next).isEqualTo(Instant.parse("2026-11-01T05:30:00Z"));
    }
}
