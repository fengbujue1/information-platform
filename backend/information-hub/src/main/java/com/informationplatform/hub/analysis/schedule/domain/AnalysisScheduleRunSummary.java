package com.informationplatform.hub.analysis.schedule.domain;

import java.time.LocalDateTime;

/** 从最近 Scheduled Batch 派生的计划运行摘要。 */
public record AnalysisScheduleRunSummary(
        /** 最近运行对应的 Batch 主键。 */
        long batchId,
        /** 最近计划点，按 UTC 表示。 */
        LocalDateTime scheduledFor,
        /** 最近 Batch 状态。 */
        String status,
        /** NOOP 时的稳定跳过原因。 */
        String skipReason,
        /** 最近 Batch 完成时间，未完成时为空。 */
        LocalDateTime completedAt) {
}
