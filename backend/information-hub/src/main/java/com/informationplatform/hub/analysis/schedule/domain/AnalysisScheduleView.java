package com.informationplatform.hub.analysis.schedule.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** Owner 安全的每日 Analysis Schedule 查询视图。 */
public record AnalysisScheduleView(
        /** Schedule 主键。 */
        long id,
        /** 用户可识别的计划名称。 */
        String name,
        /** 触发时解析 Active Version 的 Prompt Profile 主键。 */
        long promptProfileId,
        /** 是否允许 Dispatcher 触发。 */
        boolean enabled,
        /** 用户墙上时间中的每日执行时刻。 */
        LocalTime localTime,
        /** 解释墙上时间的 IANA 时区。 */
        String timezone,
        /** 最近候选回看窗口天数。 */
        int windowDays,
        /** 单批候选数量上限。 */
        int maxCandidates,
        /** 单批 Estimated Token 上限。 */
        long maxEstimatedTokens,
        /** 下一次 UTC 计划点；停用时为空。 */
        LocalDateTime nextRunAt,
        /** 创建时间，按 UTC 表示。 */
        LocalDateTime createdAt,
        /** 更新时间，按 UTC 表示。 */
        LocalDateTime updatedAt,
        /** 最近一次 Scheduled Batch；尚未运行时为空。 */
        AnalysisScheduleRunSummary lastRun) {
}
