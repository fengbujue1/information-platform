package com.informationplatform.hub.analysis.schedule.domain;

import java.time.LocalTime;

/** 创建或完整更新 Schedule 的应用层输入。 */
public record AnalysisScheduleCommand(
        /** 用户可识别名称。 */
        String name,
        /** Owner 下的 Prompt Profile 主键。 */
        long promptProfileId,
        /** 用户墙上时间；为空时使用 02:00。 */
        LocalTime localTime,
        /** IANA 时区；为空时使用账号时区。 */
        String timezone,
        /** 回看窗口天数；为空时使用 3。 */
        Integer windowDays,
        /** Candidate Limit；为空时使用 20。 */
        Integer maxCandidates,
        /** Estimated Token Budget；为空时使用 75000。 */
        Long maxEstimatedTokens) {
}
