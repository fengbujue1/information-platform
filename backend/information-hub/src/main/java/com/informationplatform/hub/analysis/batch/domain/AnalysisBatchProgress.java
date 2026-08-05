package com.informationplatform.hub.analysis.batch.domain;

/** 批次明细状态与 Provider Actual Usage 的实时聚合。 */
public record AnalysisBatchProgress(
        /** Batch Item 总数，包含 Token Budget 延后项。 */
        long itemCount,
        /** 等待 Worker 领取的 Item 数。 */
        long selectedCount,
        /** Worker 正在执行的 Item 数。 */
        long runningCount,
        /** 已成功执行的 Item 数。 */
        long succeededCount,
        /** 执行失败或结果不确定的 Item 数。 */
        long failedCount,
        /** 因复用成功 Analysis 而跳过 Provider 的 Item 数。 */
        long skippedCount,
        /** 因 Token Budget 延后的 Item 数。 */
        long deferredCount,
        /** 报告了 Provider Usage 的 Invocation 数。 */
        long reportedInvocationCount,
        /** 未报告 Provider Usage 的 Invocation 数。 */
        long unavailableInvocationCount,
        /** Provider 报告的 Actual 输入 Token 合计；完全未知时为空。 */
        Long actualInputTokens,
        /** Provider 报告的 Actual 输出 Token 合计；完全未知时为空。 */
        Long actualOutputTokens,
        /** Provider 报告的 Actual 总 Token 合计；完全未知时为空。 */
        Long actualTotalTokens) {
}
