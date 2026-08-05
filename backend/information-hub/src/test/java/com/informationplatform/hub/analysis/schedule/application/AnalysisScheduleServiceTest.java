package com.informationplatform.hub.analysis.schedule.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisScheduleMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisSchedulePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewService;
import com.informationplatform.hub.analysis.preview.domain.AnalysisPreview;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleCommand;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleTimeCalculator;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleView;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AnalysisScheduleServiceTest {

    @Test
    void createsDisabledScheduleWithFrozenDefaultsAndUserTimezone() {
        Fixture fixture = fixture();
        when(fixture.schedules.selectOwnedById(31, 7))
                .thenAnswer(invocation -> fixture.inserted);

        AnalysisScheduleView view = fixture.service.create(
                new AnalysisScheduleCommand(
                        " 每日职位 ",
                        11,
                        null,
                        null,
                        null,
                        null,
                        null),
                null);

        assertThat(view.name()).isEqualTo("每日职位");
        assertThat(view.enabled()).isFalse();
        assertThat(view.localTime()).isEqualTo(LocalTime.of(2, 0));
        assertThat(view.timezone()).isEqualTo("Asia/Shanghai");
        assertThat(view.windowDays()).isEqualTo(3);
        assertThat(view.maxCandidates()).isEqualTo(20);
        assertThat(view.maxEstimatedTokens()).isEqualTo(75_000);
        assertThat(view.nextRunAt()).isNull();
    }

    @Test
    void enablingCalculatesStrictlyFutureUtcRun() {
        Fixture fixture = fixture();
        AiAnalysisSchedulePo schedule = fixture.inserted;
        schedule.setId(31L);
        schedule.setUserId(7L);
        schedule.setName("每日职位");
        schedule.setPromptProfileId(11L);
        schedule.setEnabled(false);
        schedule.setLocalTime(LocalTime.of(2, 0));
        schedule.setTimezone("Asia/Shanghai");
        schedule.setWindowDays(3);
        schedule.setMaxCandidates(20);
        schedule.setMaxEstimatedTokens(75_000L);
        when(fixture.schedules.selectOwnedByIdForUpdate(31, 7)).thenReturn(schedule);
        when(fixture.schedules.updateStatusAndNextRun(
                        31, 7, true, java.time.LocalDateTime.of(2026, 8, 5, 18, 0)))
                .thenReturn(1);
        when(fixture.schedules.selectOwnedById(31, 7)).thenReturn(schedule);

        AnalysisScheduleView view = fixture.service.updateStatus(31, true);

        assertThat(view.enabled()).isTrue();
        assertThat(view.nextRunAt())
                .isEqualTo(java.time.LocalDateTime.of(2026, 8, 5, 18, 0));
        verify(fixture.schedules).updateStatusAndNextRun(
                31, 7, true, java.time.LocalDateTime.of(2026, 8, 5, 18, 0));
    }

    @Test
    void currentConfigPreviewReusesExistingPreviewWithoutCreatingScheduleRun() {
        Fixture fixture = fixture();
        AiAnalysisSchedulePo schedule = fixture.inserted;
        schedule.setId(31L);
        schedule.setUserId(7L);
        schedule.setPromptProfileId(11L);
        schedule.setWindowDays(3);
        schedule.setMaxCandidates(20);
        schedule.setMaxEstimatedTokens(75_000L);
        when(fixture.schedules.selectOwnedById(31, 7)).thenReturn(schedule);
        AnalysisPreview preview = mock(AnalysisPreview.class);
        when(fixture.previews.preview(11, 3, 20, 75_000L)).thenReturn(preview);

        assertThat(fixture.service.preview(31)).isSameAs(preview);

        verify(fixture.previews).preview(11, 3, 20, 75_000L);
    }

    private Fixture fixture() {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        AiAnalysisScheduleMapper schedules = mock(AiAnalysisScheduleMapper.class);
        AiPromptProfileMapper profiles = mock(AiPromptProfileMapper.class);
        AiAnalysisBatchMapper batches = mock(AiAnalysisBatchMapper.class);
        AnalysisPreviewService previews = mock(AnalysisPreviewService.class);
        when(users.requireCurrentUser()).thenReturn(
                new AuthenticatedUser(7, "owner", "Owner", "Asia/Shanghai"));
        AiPromptProfilePo profile = new AiPromptProfilePo();
        profile.setId(11L);
        profile.setUserId(7L);
        profile.setStatus("ACTIVE");
        profile.setActiveVersionId(12L);
        when(profiles.selectOwnedById(11, 7)).thenReturn(profile);
        AiAnalysisSchedulePo inserted = new AiAnalysisSchedulePo();
        when(schedules.insert(any(AiAnalysisSchedulePo.class))).thenAnswer(invocation -> {
            AiAnalysisSchedulePo source = invocation.getArgument(0);
            inserted.setId(31L);
            inserted.setUserId(source.getUserId());
            inserted.setName(source.getName());
            inserted.setPromptProfileId(source.getPromptProfileId());
            inserted.setEnabled(source.getEnabled());
            inserted.setLocalTime(source.getLocalTime());
            inserted.setTimezone(source.getTimezone());
            inserted.setWindowDays(source.getWindowDays());
            inserted.setMaxCandidates(source.getMaxCandidates());
            inserted.setMaxEstimatedTokens(source.getMaxEstimatedTokens());
            inserted.setNextRunAt(source.getNextRunAt());
            source.setId(31L);
            return 1;
        });
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-05T12:00:00Z"), ZoneOffset.UTC);
        AnalysisScheduleService service = new AnalysisScheduleService(
                users,
                schedules,
                profiles,
                batches,
                previews,
                new AnalysisScheduleTimeCalculator(),
                clock);
        return new Fixture(schedules, previews, inserted, service);
    }

    /** 单元测试使用的 Mapper、记录与被测服务集合。 */
    private record Fixture(
            /** Schedule Mapper。 */ AiAnalysisScheduleMapper schedules,
            /** 无 Provider Preview 服务。 */ AnalysisPreviewService previews,
            /** 模拟数据库回读记录。 */ AiAnalysisSchedulePo inserted,
            /** 被测应用服务。 */ AnalysisScheduleService service) {
    }
}
