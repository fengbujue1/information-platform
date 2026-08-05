package com.informationplatform.hub.analysis.batch.api.dto;

import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchProgress;

/** Batch Item 状态与 Provider Actual Usage 聚合响应。 */
public record AnalysisBatchProgressResponse(
        /** Item 总数。 */ long itemCount,
        /** 等待执行数。 */ long selectedCount,
        /** 正在执行数。 */ long runningCount,
        /** 执行成功数。 */ long succeededCount,
        /** 执行失败数。 */ long failedCount,
        /** 复用成功 Analysis 的跳过数。 */ long skippedCount,
        /** Token Budget 延后数。 */ long deferredCount,
        /** 报告 Usage 的 Invocation 数。 */ long reportedInvocationCount,
        /** 未报告 Usage 的 Invocation 数。 */ long unavailableInvocationCount,
        /** Actual 输入 Token 合计。 */ Long actualInputTokens,
        /** Actual 输出 Token 合计。 */ Long actualOutputTokens,
        /** Actual 总 Token 合计。 */ Long actualTotalTokens) {

    public static AnalysisBatchProgressResponse from(AnalysisBatchProgress progress) {
        return new AnalysisBatchProgressResponse(
                progress.itemCount(),
                progress.selectedCount(),
                progress.runningCount(),
                progress.succeededCount(),
                progress.failedCount(),
                progress.skippedCount(),
                progress.deferredCount(),
                progress.reportedInvocationCount(),
                progress.unavailableInvocationCount(),
                progress.actualInputTokens(),
                progress.actualOutputTokens(),
                progress.actualTotalTokens());
    }
}
