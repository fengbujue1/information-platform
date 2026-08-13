package com.informationplatform.hub.analysis.schedule.application;

import com.informationplatform.hub.analysis.batch.application.AnalysisBatchTransactionService;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisBatchMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.mapper.AiAnalysisScheduleMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisSchedulePo;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewLimitPolicy;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewNotFoundException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewRequestException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewService;
import com.informationplatform.hub.analysis.preview.domain.AnalysisPreviewLimits;
import com.informationplatform.hub.analysis.preview.domain.ResolvedAnalysisPreview;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleDispatchResult;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleTimeCalculator;
import com.informationplatform.hub.analysis.schedule.infrastructure.config.AnalysisScheduleProperties;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 以短数据库事务锁定 due Schedule、创建 Batch 并原子推进 nextRunAt。 */
@Service
public class AnalysisScheduleTransactionService {

    /** Schedule due-row 锁定与推进。 */
    private final AiAnalysisScheduleMapper scheduleMapper;
    /** Scheduled 幂等、overlap 和运行查询。 */
    private final AiAnalysisBatchMapper batchMapper;
    /** 复用 Candidate Resolver、Estimate 与 Budget Guard。 */
    private final AnalysisPreviewService previewService;
    /** 调度执行前按当前平台上限复核已有配置。 */
    private final AnalysisPreviewLimitPolicy limitPolicy;
    /** 复用 Manual Batch 的冻结写入和 Worker 数据结构。 */
    private final AnalysisBatchTransactionService batchTransactionService;
    /** IANA 时区与 DST 计划点计算。 */
    private final AnalysisScheduleTimeCalculator timeCalculator;
    /** misfire grace 等冻结运行参数。 */
    private final AnalysisScheduleProperties properties;

    public AnalysisScheduleTransactionService(
            AiAnalysisScheduleMapper scheduleMapper,
            AiAnalysisBatchMapper batchMapper,
            AnalysisPreviewService previewService,
            AnalysisPreviewLimitPolicy limitPolicy,
            AnalysisBatchTransactionService batchTransactionService,
            AnalysisScheduleTimeCalculator timeCalculator,
            AnalysisScheduleProperties properties) {
        this.scheduleMapper = scheduleMapper;
        this.batchMapper = batchMapper;
        this.previewService = previewService;
        this.limitPolicy = limitPolicy;
        this.batchTransactionService = batchTransactionService;
        this.timeCalculator = timeCalculator;
        this.properties = properties;
    }

    /**
     * 锁定并处理一个到期 Schedule。
     *
     * <p>候选解析不调用 Provider；Batch/NOOP 与 nextRunAt 必须一起提交或一起回滚。
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public AnalysisScheduleDispatchResult dispatchNext(Instant now) {
        LocalDateTime nowUtc = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        AiAnalysisSchedulePo schedule = scheduleMapper.selectNextDueForUpdate(nowUtc);
        if (schedule == null) {
            return AnalysisScheduleDispatchResult.idle();
        }
        if (!Boolean.TRUE.equals(schedule.getEnabled()) || schedule.getNextRunAt() == null) {
            return new AnalysisScheduleDispatchResult(
                    true, schedule.getId(), null, "DISABLED");
        }

        LocalDateTime scheduledFor = schedule.getNextRunAt();
        Instant scheduledInstant = scheduledFor.toInstant(ZoneOffset.UTC);
        AiAnalysisBatchPo existing =
                batchMapper.selectScheduled(schedule.getId(), scheduledFor);
        if (existing != null) {
            // 唯一键已存在时只推进日程，避免因重启或历史状态重复建批。
            advance(schedule, now);
            return new AnalysisScheduleDispatchResult(
                    true, schedule.getId(), existing.getId(), "IDEMPOTENT_REUSE");
        }

        ResolvedAnalysisPreview resolved;
        try {
            AnalysisPreviewLimits limits = limitPolicy.resolve(
                    schedule.getWindowDays(),
                    schedule.getMaxCandidates(),
                    schedule.getMaxEstimatedTokens());
            resolved = previewService.resolveScheduled(
                    schedule.getUserId(),
                    schedule.getPromptProfileId(),
                    scheduledInstant,
                    limits);
        } catch (AnalysisPreviewRequestException | AnalysisPreviewNotFoundException exception) {
            // 非法运行配置不能构造满足非空 FK 的 Batch，安全跳过且不持续卡住 due row。
            advance(schedule, now);
            return new AnalysisScheduleDispatchResult(
                    true, schedule.getId(), null, "INVALID_CONFIGURATION");
        }

        String forcedSkipReason = null;
        String outcome = "CREATED";
        if (scheduledInstant.isBefore(now.minus(properties.getMisfireGrace()))) {
            forcedSkipReason = "MISFIRE";
            outcome = "MISFIRE";
        } else if (batchMapper.selectActiveScheduled(schedule.getId()) != null) {
            forcedSkipReason = "CONCURRENT_RUN";
            outcome = "OVERLAP";
        }

        AiAnalysisBatchPo batch = batchTransactionService.createScheduled(
                schedule, scheduledFor, resolved, forcedSkipReason);
        advance(schedule, now);
        return new AnalysisScheduleDispatchResult(
                true, schedule.getId(), batch.getId(), outcome);
    }

    /** 从当前真实时刻计算下一未来计划点，确保 misfire 不循环追赶历史。 */
    private void advance(AiAnalysisSchedulePo schedule, Instant now) {
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(schedule.getTimezone());
        } catch (RuntimeException exception) {
            schedule.setEnabled(false);
            schedule.setNextRunAt(null);
            if (scheduleMapper.updateStatusAndNextRun(
                            schedule.getId(), schedule.getUserId(), false, null)
                    != 1) {
                throw new AnalysisSchedulePersistenceException(
                        "Invalid Schedule could not be disabled");
            }
            return;
        }
        Instant next = timeCalculator.nextRunAfter(schedule.getLocalTime(), zoneId, now);
        schedule.setNextRunAt(LocalDateTime.ofInstant(next, ZoneOffset.UTC));
        if (scheduleMapper.updateNextRunAt(schedule.getId(), schedule.getNextRunAt()) != 1) {
            throw new AnalysisSchedulePersistenceException(
                    "Analysis Schedule nextRunAt update affected no row");
        }
    }
}
