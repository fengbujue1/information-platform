package com.informationplatform.hub.analysis.batch.domain;

import java.time.LocalDateTime;
import java.util.List;

/** Owner 安全的冻结 Analysis Batch 查询视图。 */
public record AnalysisBatchView(
        /** Batch 主键。 */
        long id,
        /** 触发类型，当前 TASK 只创建 MANUAL。 */
        String triggerType,
        /** Prompt Profile 主键。 */
        long promptProfileId,
        /** 冻结 Prompt Version 主键。 */
        long promptVersionId,
        /** 信息类型，当前真实实现为 JOB。 */
        String informationType,
        /** 冻结 Definition Key。 */
        String analysisDefinitionKey,
        /** 冻结 Definition 版本。 */
        int analysisDefinitionVersion,
        /** 候选窗口口径，当前固定 FIRST_INGESTED。 */
        String windowBasis,
        /** 请求回看窗口天数。 */
        int requestedWindowDays,
        /** UTC 半开窗口起点。 */
        LocalDateTime windowStart,
        /** UTC 半开窗口终点。 */
        LocalDateTime windowEnd,
        /** 请求 Candidate Limit。 */
        int requestedMaxCandidates,
        /** 请求 Estimated Token Budget。 */
        long requestedTokenBudget,
        /** 窗口内总数。 */
        int totalInWindow,
        /** 未成功分析的候选数。 */
        int eligibleCount,
        /** Preview 时已成功分析数。 */
        int alreadyAnalyzedCount,
        /** 预算内执行候选数。 */
        int selectedCount,
        /** Candidate Limit 延后数。 */
        int deferredByItemLimitCount,
        /** Token Budget 延后数。 */
        int deferredByTokenBudgetCount,
        /** 预算内 Estimated 输入 Token。 */
        Long estimatedInputTokens,
        /** 预算内 Estimated 输出 Token。 */
        Long estimatedOutputTokens,
        /** 预算内 Estimated 总 Token。 */
        Long estimatedTotalTokens,
        /** Estimate 算法版本。 */
        String estimateMethod,
        /** Batch 状态。 */
        String status,
        /** NOOP 稳定原因。 */
        String skipReason,
        /** UTC 开始时间。 */
        LocalDateTime startedAt,
        /** UTC 完成时间。 */
        LocalDateTime completedAt,
        /** UTC 创建时间。 */
        LocalDateTime createdAt,
        /** UTC 更新时间。 */
        LocalDateTime updatedAt,
        /** 实时状态和 Actual Usage 聚合。 */
        AnalysisBatchProgress progress,
        /** Detail 查询返回的有序 Items；列表查询为空列表。 */
        List<AnalysisBatchItemView> items) {

    public AnalysisBatchView {
        items = List.copyOf(items);
    }
}
