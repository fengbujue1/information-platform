package com.informationplatform.hub.analysis.usage.api.dto;

import com.informationplatform.hub.analysis.usage.domain.AnalysisUsageSummary;

/** 当前 Owner 的今日、本月和累计 Actual Usage HTTP 响应。 */
public record AnalysisUsageResponse(
        /** 自然日和自然月边界使用的用户 IANA 时区。 */
        String timezone,
        /** 今日 Actual Usage。 */
        AnalysisUsagePeriodResponse today,
        /** 本月 Actual Usage。 */
        AnalysisUsagePeriodResponse month,
        /** 累计 Actual Usage。 */
        AnalysisUsagePeriodResponse allTime) {

    public static AnalysisUsageResponse from(AnalysisUsageSummary summary) {
        return new AnalysisUsageResponse(
                summary.timezone(),
                AnalysisUsagePeriodResponse.from(summary.today()),
                AnalysisUsagePeriodResponse.from(summary.month()),
                AnalysisUsagePeriodResponse.from(summary.allTime()));
    }
}
