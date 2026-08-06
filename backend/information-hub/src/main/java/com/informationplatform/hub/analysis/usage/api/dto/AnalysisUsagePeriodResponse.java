package com.informationplatform.hub.analysis.usage.api.dto;

import com.informationplatform.hub.analysis.usage.domain.AnalysisUsagePeriod;
import java.time.Instant;

/** 一个用户时间范围内的 Provider Actual Usage HTTP 响应。 */
public record AnalysisUsagePeriodResponse(
        /** UTC 统计范围起点；累计范围为空。 */
        Instant periodStart,
        /** UTC 统计范围终点；累计范围为空。 */
        Instant periodEnd,
        /** 范围内全部 Invocation 数。 */
        long invocationCount,
        /** Provider 已报告 Usage 的 Invocation 数。 */
        long reportedInvocationCount,
        /** Provider 未报告 Usage 的 Invocation 数。 */
        long unavailableInvocationCount,
        /** Actual 输入 Token 合计，完全未知时为空。 */
        Long actualInputTokens,
        /** Actual 输出 Token 合计，完全未知时为空。 */
        Long actualOutputTokens,
        /** Actual 总 Token 合计，完全未知时为空。 */
        Long actualTotalTokens) {

    public static AnalysisUsagePeriodResponse from(AnalysisUsagePeriod period) {
        return new AnalysisUsagePeriodResponse(
                period.periodStart(),
                period.periodEnd(),
                period.invocationCount(),
                period.reportedInvocationCount(),
                period.unavailableInvocationCount(),
                period.actualInputTokens(),
                period.actualOutputTokens(),
                period.actualTotalTokens());
    }
}
