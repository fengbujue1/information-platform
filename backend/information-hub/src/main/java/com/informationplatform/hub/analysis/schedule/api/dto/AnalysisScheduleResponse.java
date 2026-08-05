package com.informationplatform.hub.analysis.schedule.api.dto;

import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleView;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 每日 Analysis Schedule 的 Owner 安全响应。 */
public record AnalysisScheduleResponse(
        /** Schedule 主键。 */
        long id,
        /** 用户可识别名称。 */
        String name,
        /** Prompt Profile 主键。 */
        long promptProfileId,
        /** 是否启用。 */
        boolean enabled,
        /** 用户墙上时间中的每日执行时刻。 */
        LocalTime localTime,
        /** IANA 时区。 */
        String timezone,
        /** 回看窗口天数。 */
        int windowDays,
        /** Candidate Limit。 */
        int maxCandidates,
        /** Estimated Token Budget。 */
        long maxEstimatedTokens,
        /** 下一次 UTC 计划点；停用时为空。 */
        LocalDateTime nextRunAt,
        /** UTC 创建时间。 */
        LocalDateTime createdAt,
        /** UTC 更新时间。 */
        LocalDateTime updatedAt,
        /** 最近一次运行摘要；尚未运行时为空。 */
        AnalysisScheduleRunResponse lastRun) {

    public static AnalysisScheduleResponse from(AnalysisScheduleView view) {
        return new AnalysisScheduleResponse(
                view.id(),
                view.name(),
                view.promptProfileId(),
                view.enabled(),
                view.localTime(),
                view.timezone(),
                view.windowDays(),
                view.maxCandidates(),
                view.maxEstimatedTokens(),
                view.nextRunAt(),
                view.createdAt(),
                view.updatedAt(),
                view.lastRun() == null
                        ? null : AnalysisScheduleRunResponse.from(view.lastRun()));
    }
}
