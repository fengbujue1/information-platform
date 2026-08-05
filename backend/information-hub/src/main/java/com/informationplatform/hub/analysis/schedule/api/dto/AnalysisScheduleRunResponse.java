package com.informationplatform.hub.analysis.schedule.api.dto;

import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleRunSummary;
import java.time.LocalDateTime;

/** 最近一次 Scheduled Batch 的精简响应。 */
public record AnalysisScheduleRunResponse(
        /** 最近运行对应的 Batch 主键。 */
        long batchId,
        /** 最近 UTC 计划点。 */
        LocalDateTime scheduledFor,
        /** 最近 Batch 状态。 */
        String status,
        /** NOOP 时的稳定跳过原因。 */
        String skipReason,
        /** 最近 Batch 完成时间。 */
        LocalDateTime completedAt) {

    public static AnalysisScheduleRunResponse from(AnalysisScheduleRunSummary summary) {
        return new AnalysisScheduleRunResponse(
                summary.batchId(),
                summary.scheduledFor(),
                summary.status(),
                summary.skipReason(),
                summary.completedAt());
    }
}
