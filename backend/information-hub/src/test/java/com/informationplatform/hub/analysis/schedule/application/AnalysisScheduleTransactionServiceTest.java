package com.informationplatform.hub.analysis.schedule.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.batch.application.AnalysisBatchTransactionService;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisScheduleMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisSchedulePo;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewService;
import com.informationplatform.hub.analysis.preview.domain.AnalysisPreviewLimits;
import com.informationplatform.hub.analysis.preview.domain.ResolvedAnalysisPreview;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleDispatchResult;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleTimeCalculator;
import com.informationplatform.hub.analysis.schedule.infrastructure.config.AnalysisScheduleProperties;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

class AnalysisScheduleTransactionServiceTest {

    @Test
    void dispatchUsesRepeatableReadTransaction() throws Exception {
        Transactional transactional = AnalysisScheduleTransactionService.class
                .getMethod("dispatchNext", Instant.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
    }

    @Test
    void createsScheduledBatchAndAdvancesNextRun() {
        Fixture fixture = fixture(LocalDateTime.of(2026, 8, 5, 12, 0));

        AnalysisScheduleDispatchResult result =
                fixture.service.dispatchNext(Instant.parse("2026-08-05T12:01:00Z"));

        assertThat(result.outcome()).isEqualTo("CREATED");
        assertThat(result.batchId()).isEqualTo(51);
        verify(fixture.batchTransactions).createScheduled(
                eq(fixture.schedule),
                eq(LocalDateTime.of(2026, 8, 5, 12, 0)),
                eq(fixture.resolved),
                isNull());
        assertThat(fixture.schedule.getNextRunAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 5, 18, 0));
    }

    @Test
    void createsMisfireNoopAndDoesNotCatchUpHistoricalPoints() {
        Fixture fixture = fixture(LocalDateTime.of(2026, 8, 5, 11, 50));

        AnalysisScheduleDispatchResult result =
                fixture.service.dispatchNext(Instant.parse("2026-08-05T12:00:00Z"));

        assertThat(result.outcome()).isEqualTo("MISFIRE");
        verify(fixture.batchTransactions).createScheduled(
                eq(fixture.schedule),
                eq(LocalDateTime.of(2026, 8, 5, 11, 50)),
                eq(fixture.resolved),
                eq("MISFIRE"));
        assertThat(fixture.schedule.getNextRunAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 5, 18, 0));
    }

    @Test
    void createsConcurrentRunNoopWhenPreviousBatchIsActive() {
        Fixture fixture = fixture(LocalDateTime.of(2026, 8, 5, 12, 0));
        when(fixture.batches.selectActiveScheduled(31)).thenReturn(new AiAnalysisBatchPo());

        AnalysisScheduleDispatchResult result =
                fixture.service.dispatchNext(Instant.parse("2026-08-05T12:01:00Z"));

        assertThat(result.outcome()).isEqualTo("OVERLAP");
        verify(fixture.batchTransactions).createScheduled(
                eq(fixture.schedule),
                any(LocalDateTime.class),
                eq(fixture.resolved),
                eq("CONCURRENT_RUN"));
    }

    @Test
    void reusesExistingScheduledForWithoutResolvingCandidatesAgain() {
        Fixture fixture = fixture(LocalDateTime.of(2026, 8, 5, 12, 0));
        AiAnalysisBatchPo existing = new AiAnalysisBatchPo();
        existing.setId(61L);
        when(fixture.batches.selectScheduled(
                        31, LocalDateTime.of(2026, 8, 5, 12, 0)))
                .thenReturn(existing);

        AnalysisScheduleDispatchResult result =
                fixture.service.dispatchNext(Instant.parse("2026-08-05T12:01:00Z"));

        assertThat(result.outcome()).isEqualTo("IDEMPOTENT_REUSE");
        assertThat(result.batchId()).isEqualTo(61);
        verify(fixture.previews, never()).resolveScheduled(
                anyLong(),
                anyLong(),
                any(Instant.class),
                any(AnalysisPreviewLimits.class));
    }

    private Fixture fixture(LocalDateTime scheduledFor) {
        AiAnalysisScheduleMapper schedules = mock(AiAnalysisScheduleMapper.class);
        AiAnalysisBatchMapper batches = mock(AiAnalysisBatchMapper.class);
        AnalysisPreviewService previews = mock(AnalysisPreviewService.class);
        AnalysisBatchTransactionService batchTransactions =
                mock(AnalysisBatchTransactionService.class);
        AiAnalysisSchedulePo schedule = schedule(scheduledFor);
        ResolvedAnalysisPreview resolved = mock(ResolvedAnalysisPreview.class);
        AiAnalysisBatchPo created = new AiAnalysisBatchPo();
        created.setId(51L);
        when(schedules.selectNextDueForUpdate(any(LocalDateTime.class)))
                .thenReturn(schedule);
        when(schedules.updateNextRunAt(eq(31L), any(LocalDateTime.class))).thenReturn(1);
        when(previews.resolveScheduled(
                        eq(7L),
                        eq(11L),
                        any(Instant.class),
                        any(AnalysisPreviewLimits.class)))
                .thenReturn(resolved);
        when(batchTransactions.createScheduled(
                        eq(schedule),
                        eq(scheduledFor),
                        eq(resolved),
                        nullable(String.class)))
                .thenReturn(created);
        AnalysisScheduleProperties properties = new AnalysisScheduleProperties();
        AnalysisScheduleTransactionService service = new AnalysisScheduleTransactionService(
                schedules,
                batches,
                previews,
                batchTransactions,
                new AnalysisScheduleTimeCalculator(),
                properties);
        return new Fixture(
                batches, previews, batchTransactions, schedule, resolved, service);
    }

    private AiAnalysisSchedulePo schedule(LocalDateTime scheduledFor) {
        AiAnalysisSchedulePo schedule = new AiAnalysisSchedulePo();
        schedule.setId(31L);
        schedule.setUserId(7L);
        schedule.setPromptProfileId(11L);
        schedule.setEnabled(true);
        schedule.setLocalTime(LocalTime.of(2, 0));
        schedule.setTimezone("Asia/Shanghai");
        schedule.setWindowDays(3);
        schedule.setMaxCandidates(20);
        schedule.setMaxEstimatedTokens(75_000L);
        schedule.setNextRunAt(scheduledFor);
        return schedule;
    }

    /** 单元测试的 Mapper、协作者与被测事务服务集合。 */
    private record Fixture(
            /** Batch Mapper。 */ AiAnalysisBatchMapper batches,
            /** Preview 服务。 */ AnalysisPreviewService previews,
            /** Batch 冻结事务服务。 */ AnalysisBatchTransactionService batchTransactions,
            /** 被锁定的 Schedule。 */ AiAnalysisSchedulePo schedule,
            /** 候选解析结果。 */ ResolvedAnalysisPreview resolved,
            /** 被测事务服务。 */ AnalysisScheduleTransactionService service) {
    }
}
