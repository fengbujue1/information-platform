package com.informationplatform.hub.analysis.usage.domain;

import java.time.Instant;

/** 一个用户时间范围内的 Provider Actual Usage 汇总。 */
public record AnalysisUsagePeriod(
        /** 统计范围起点；累计范围为空。 */
        Instant periodStart,
        /** 统计范围终点；累计范围为空。 */
        Instant periodEnd,
        /** 范围内全部 Invocation 数。 */
        long invocationCount,
        /** Provider 已报告 Usage 的 Invocation 数。 */
        long reportedInvocationCount,
        /** Provider 未报告 Usage 的 Invocation 数。 */
        long unavailableInvocationCount,
        /** Provider 报告的 Actual 输入 Token 合计，完全未知时为空。 */
        Long actualInputTokens,
        /** Provider 报告的 Actual 输出 Token 合计，完全未知时为空。 */
        Long actualOutputTokens,
        /** Provider 报告的 Actual 总 Token 合计，完全未知时为空。 */
        Long actualTotalTokens) {
}
