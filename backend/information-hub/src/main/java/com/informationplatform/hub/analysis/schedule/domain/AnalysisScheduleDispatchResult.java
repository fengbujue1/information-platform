package com.informationplatform.hub.analysis.schedule.domain;

/** Dispatcher 单次事务处理一个到期 Schedule 的脱敏结果。 */
public record AnalysisScheduleDispatchResult(
        /** 是否找到并处理了到期 Schedule。 */
        boolean processed,
        /** 被处理的 Schedule 主键；未找到时为空。 */
        Long scheduleId,
        /** 创建或复用的 Batch 主键；配置不可用时为空。 */
        Long batchId,
        /** 调度结果，例如 CREATED、MISFIRE、OVERLAP 或 INVALID_CONFIGURATION。 */
        String outcome) {

    public static AnalysisScheduleDispatchResult idle() {
        return new AnalysisScheduleDispatchResult(false, null, null, "IDLE");
    }
}
