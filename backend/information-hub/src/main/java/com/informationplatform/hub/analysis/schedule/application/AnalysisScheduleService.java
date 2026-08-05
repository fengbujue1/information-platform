package com.informationplatform.hub.analysis.schedule.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisScheduleMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiPromptProfileMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisSchedulePo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewService;
import com.informationplatform.hub.analysis.preview.domain.AnalysisPreview;
import com.informationplatform.hub.analysis.preview.domain.AnalysisPreviewLimits;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleCommand;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleRunSummary;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleTimeCalculator;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleView;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 实现 Owner 隔离的每日 Schedule 配置、启停、查询和配置 Preview。 */
@Service
public class AnalysisScheduleService {

    private static final LocalTime DEFAULT_LOCAL_TIME = LocalTime.of(2, 0);
    private static final int MAX_NAME_CODE_POINTS = 255;

    /** 从 Session 获取可信 Owner 和账号默认时区。 */
    private final CurrentUserProvider currentUserProvider;
    /** Schedule 持久化与行锁。 */
    private final AiAnalysisScheduleMapper scheduleMapper;
    /** Prompt Profile Owner 和可执行配置校验。 */
    private final AiPromptProfileMapper profileMapper;
    /** 最近 Scheduled Batch 派生查询。 */
    private final AiAnalysisBatchMapper batchMapper;
    /** Test Current Config 复用无 Provider Preview。 */
    private final AnalysisPreviewService previewService;
    /** IANA 时区和 DST 安全的未来计划点计算。 */
    private final AnalysisScheduleTimeCalculator timeCalculator;
    /** 可替换 UTC 时钟。 */
    private final Clock clock;

    public AnalysisScheduleService(
            CurrentUserProvider currentUserProvider,
            AiAnalysisScheduleMapper scheduleMapper,
            AiPromptProfileMapper profileMapper,
            AiAnalysisBatchMapper batchMapper,
            AnalysisPreviewService previewService,
            AnalysisScheduleTimeCalculator timeCalculator,
            Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.scheduleMapper = scheduleMapper;
        this.profileMapper = profileMapper;
        this.batchMapper = batchMapper;
        this.previewService = previewService;
        this.timeCalculator = timeCalculator;
        this.clock = clock;
    }

    /** 创建默认关闭的 Schedule；数据库唯一键最终处理并发同名创建。 */
    @Transactional
    public AnalysisScheduleView create(
            AnalysisScheduleCommand command, Boolean enabled) {
        AuthenticatedUser user = currentUserProvider.requireCurrentUser();
        ResolvedConfiguration configuration = resolveConfiguration(command, user);
        requireExecutableProfile(command.promptProfileId(), user.id());

        AiAnalysisSchedulePo schedule = new AiAnalysisSchedulePo();
        schedule.setUserId(user.id());
        apply(schedule, configuration);
        schedule.setEnabled(Boolean.TRUE.equals(enabled));
        schedule.setNextRunAt(schedule.getEnabled()
                ? nextRun(configuration.localTime(), configuration.zoneId(), clock.instant())
                : null);
        try {
            if (scheduleMapper.insert(schedule) != 1 || schedule.getId() == null) {
                throw new AnalysisSchedulePersistenceException(
                        "Analysis Schedule insert affected no row");
            }
        } catch (DuplicateKeyException exception) {
            throw new AnalysisScheduleConflictException(
                    "An Analysis Schedule with the same name already exists", exception);
        }
        return toView(scheduleMapper.selectOwnedById(schedule.getId(), user.id()));
    }

    /** 完整替换 Schedule 配置；保持当前 enabled 状态并重新计算未来计划点。 */
    @Transactional
    public AnalysisScheduleView update(long scheduleId, AnalysisScheduleCommand command) {
        AuthenticatedUser user = currentUserProvider.requireCurrentUser();
        AiAnalysisSchedulePo schedule = requireOwnedForUpdate(scheduleId, user.id());
        ResolvedConfiguration configuration = resolveConfiguration(command, user);
        requireExecutableProfile(command.promptProfileId(), user.id());
        apply(schedule, configuration);
        schedule.setNextRunAt(Boolean.TRUE.equals(schedule.getEnabled())
                ? nextRun(configuration.localTime(), configuration.zoneId(), clock.instant())
                : null);
        try {
            if (scheduleMapper.updateById(schedule) != 1) {
                throw new AnalysisSchedulePersistenceException(
                        "Analysis Schedule update affected no row");
            }
        } catch (DuplicateKeyException exception) {
            throw new AnalysisScheduleConflictException(
                    "An Analysis Schedule with the same name already exists", exception);
        }
        return toView(scheduleMapper.selectOwnedById(scheduleId, user.id()));
    }

    /** 启用时计算下一未来计划点；停用时原子清空 nextRunAt。 */
    @Transactional
    public AnalysisScheduleView updateStatus(long scheduleId, boolean enabled) {
        AuthenticatedUser user = currentUserProvider.requireCurrentUser();
        AiAnalysisSchedulePo schedule = requireOwnedForUpdate(scheduleId, user.id());
        if (enabled) {
            requireExecutableProfile(schedule.getPromptProfileId(), user.id());
            ZoneId zoneId = requireZoneId(schedule.getTimezone());
            schedule.setNextRunAt(nextRun(schedule.getLocalTime(), zoneId, clock.instant()));
        } else {
            schedule.setNextRunAt(null);
        }
        schedule.setEnabled(enabled);
        if (scheduleMapper.updateStatusAndNextRun(
                        schedule.getId(),
                        schedule.getUserId(),
                        enabled,
                        schedule.getNextRunAt())
                != 1) {
            throw new AnalysisSchedulePersistenceException(
                    "Analysis Schedule status update affected no row");
        }
        return toView(scheduleMapper.selectOwnedById(scheduleId, user.id()));
    }

    /** 返回当前 Owner 的全部 Schedule 和各自最近运行摘要。 */
    @Transactional(readOnly = true)
    public List<AnalysisScheduleView> list() {
        long userId = currentUserProvider.requireCurrentUser().id();
        return scheduleMapper.selectList(
                        Wrappers.<AiAnalysisSchedulePo>lambdaQuery()
                                .eq(AiAnalysisSchedulePo::getUserId, userId)
                                .orderByDesc(
                                        AiAnalysisSchedulePo::getCreatedAt,
                                        AiAnalysisSchedulePo::getId))
                .stream()
                .map(this::toView)
                .toList();
    }

    /** 返回当前 Owner 的单个 Schedule；跨 Owner 与不存在统一为 404。 */
    @Transactional(readOnly = true)
    public AnalysisScheduleView get(long scheduleId) {
        long userId = currentUserProvider.requireCurrentUser().id();
        AiAnalysisSchedulePo schedule = scheduleMapper.selectOwnedById(scheduleId, userId);
        if (schedule == null) {
            throw notFound();
        }
        return toView(schedule);
    }

    /** 以当前时刻测试持久化配置，不创建 Batch、Analysis 或 Invocation。 */
    @Transactional(readOnly = true)
    public AnalysisPreview preview(long scheduleId) {
        long userId = currentUserProvider.requireCurrentUser().id();
        AiAnalysisSchedulePo schedule = scheduleMapper.selectOwnedById(scheduleId, userId);
        if (schedule == null) {
            throw notFound();
        }
        return previewService.preview(
                schedule.getPromptProfileId(),
                schedule.getWindowDays(),
                schedule.getMaxCandidates(),
                schedule.getMaxEstimatedTokens());
    }

    private ResolvedConfiguration resolveConfiguration(
            AnalysisScheduleCommand command, AuthenticatedUser user) {
        String name = normalizeName(command.name());
        LocalTime localTime =
                command.localTime() == null ? DEFAULT_LOCAL_TIME : command.localTime();
        String timezone = command.timezone() == null || command.timezone().isBlank()
                ? user.timezone()
                : command.timezone().trim();
        ZoneId zoneId = requireZoneId(timezone);
        AnalysisPreviewLimits limits = AnalysisPreviewLimits.resolve(
                command.windowDays(),
                command.maxCandidates(),
                command.maxEstimatedTokens());
        return new ResolvedConfiguration(
                name, command.promptProfileId(), localTime, timezone, zoneId, limits);
    }

    private void apply(
            AiAnalysisSchedulePo schedule, ResolvedConfiguration configuration) {
        schedule.setName(configuration.name());
        schedule.setPromptProfileId(configuration.promptProfileId());
        schedule.setLocalTime(configuration.localTime());
        schedule.setTimezone(configuration.timezone());
        schedule.setWindowDays(configuration.limits().windowDays());
        schedule.setMaxCandidates(configuration.limits().maxCandidates());
        schedule.setMaxEstimatedTokens(configuration.limits().maxEstimatedTokens());
    }

    private void requireExecutableProfile(long profileId, long userId) {
        if (profileId <= 0) {
            throw request(
                    "ANALYSIS_SCHEDULE_PROFILE_ID_INVALID",
                    "promptProfileId must be positive");
        }
        AiPromptProfilePo profile = profileMapper.selectOwnedById(profileId, userId);
        if (profile == null) {
            throw new AnalysisScheduleNotFoundException(
                    "Prompt Profile does not exist");
        }
        if (!"ACTIVE".equals(profile.getStatus())) {
            throw request(
                    "ANALYSIS_SCHEDULE_PROFILE_DISABLED",
                    "Prompt Profile must be active");
        }
        if (profile.getActiveVersionId() == null) {
            throw request(
                    "ANALYSIS_SCHEDULE_ACTIVE_VERSION_REQUIRED",
                    "Prompt Profile must have an active version");
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw request(
                    "ANALYSIS_SCHEDULE_NAME_REQUIRED",
                    "Analysis Schedule name is required");
        }
        if (normalized.codePointCount(0, normalized.length()) > MAX_NAME_CODE_POINTS) {
            throw request(
                    "ANALYSIS_SCHEDULE_NAME_TOO_LONG",
                    "Analysis Schedule name must not exceed 255 characters");
        }
        return normalized;
    }

    private ZoneId requireZoneId(String timezone) {
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            if (!ZoneId.getAvailableZoneIds().contains(timezone)
                    && !"UTC".equals(timezone)
                    && !"GMT".equals(timezone)) {
                throw new IllegalArgumentException("Timezone is not an IANA region");
            }
            return zoneId;
        } catch (RuntimeException exception) {
            throw request(
                    "ANALYSIS_SCHEDULE_TIMEZONE_INVALID",
                    "timezone must be a valid IANA Zone ID");
        }
    }

    private LocalDateTime nextRun(LocalTime localTime, ZoneId zoneId, Instant after) {
        return LocalDateTime.ofInstant(
                timeCalculator.nextRunAfter(localTime, zoneId, after), ZoneOffset.UTC);
    }

    private AiAnalysisSchedulePo requireOwnedForUpdate(long scheduleId, long userId) {
        AiAnalysisSchedulePo schedule =
                scheduleMapper.selectOwnedByIdForUpdate(scheduleId, userId);
        if (schedule == null) {
            throw notFound();
        }
        return schedule;
    }

    private AnalysisScheduleView toView(AiAnalysisSchedulePo schedule) {
        AiAnalysisBatchPo latest = batchMapper.selectLatestScheduled(schedule.getId());
        AnalysisScheduleRunSummary lastRun = latest == null
                ? null
                : new AnalysisScheduleRunSummary(
                        latest.getId(),
                        latest.getScheduledFor(),
                        latest.getStatus(),
                        latest.getSkipReason(),
                        latest.getCompletedAt());
        return new AnalysisScheduleView(
                schedule.getId(),
                schedule.getName(),
                schedule.getPromptProfileId(),
                Boolean.TRUE.equals(schedule.getEnabled()),
                schedule.getLocalTime(),
                schedule.getTimezone(),
                schedule.getWindowDays(),
                schedule.getMaxCandidates(),
                schedule.getMaxEstimatedTokens(),
                schedule.getNextRunAt(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt(),
                lastRun);
    }

    private AnalysisScheduleNotFoundException notFound() {
        return new AnalysisScheduleNotFoundException(
                "Analysis Schedule does not exist");
    }

    private AnalysisScheduleRequestException request(String code, String message) {
        return new AnalysisScheduleRequestException(code, message);
    }

    /** 已规范化并通过硬上限校验的 Schedule 配置。 */
    private record ResolvedConfiguration(
            /** 规范化名称。 */ String name,
            /** Prompt Profile 主键。 */ long promptProfileId,
            /** 每日墙上时间。 */ LocalTime localTime,
            /** IANA 时区原始名称。 */ String timezone,
            /** 已解析时区。 */ ZoneId zoneId,
            /** 冻结候选与预算限制。 */ AnalysisPreviewLimits limits) {
    }
}
