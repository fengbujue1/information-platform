package com.informationplatform.hub.analysis.usage.domain;

/** 当前 Owner 按账号时区统计的今日、本月和累计 Actual Usage。 */
public record AnalysisUsageSummary(
        /** 计算自然日和自然月边界的用户 IANA 时区。 */
        String timezone,
        /** 今日 Actual Usage。 */
        AnalysisUsagePeriod today,
        /** 本月 Actual Usage。 */
        AnalysisUsagePeriod month,
        /** 不限制时间的累计 Actual Usage。 */
        AnalysisUsagePeriod allTime) {
}
