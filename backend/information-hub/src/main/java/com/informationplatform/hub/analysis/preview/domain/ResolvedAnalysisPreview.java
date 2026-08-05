package com.informationplatform.hub.analysis.preview.domain;

import java.time.Instant;
import java.util.List;

/** Preview 与 Confirm 共用的完整确定性候选解析结果。 */
public record ResolvedAnalysisPreview(
        /** 当前 Owner 主键。 */
        long userId,
        /** Prompt Profile 主键。 */
        long promptProfileId,
        /** 冻结的 Prompt Version 主键。 */
        long promptVersionId,
        /** Analysis Definition 对应的信息类型。 */
        String informationType,
        /** 冻结的 Definition Key。 */
        String definitionKey,
        /** 冻结的 Definition 版本。 */
        int definitionVersion,
        /** UTC 半开窗口起点。 */
        Instant windowStart,
        /** UTC 半开窗口终点。 */
        Instant windowEnd,
        /** 请求窗口天数。 */
        int windowDays,
        /** 请求 Candidate Limit。 */
        int maxCandidates,
        /** 请求 Estimated Token Budget。 */
        long maxEstimatedTokens,
        /** 窗口内总数。 */
        long totalInWindow,
        /** 未成功分析的候选数。 */
        long eligibleCount,
        /** 已成功分析数。 */
        long alreadyAnalyzedCount,
        /** 最终选择执行的候选数。 */
        long selectedCount,
        /** Candidate Limit 外候选数。 */
        long deferredByItemLimitCount,
        /** Token Budget 外候选数。 */
        long deferredByTokenBudgetCount,
        /** 选中候选 Estimated 输入 Token 总数。 */
        long estimatedInputTokens,
        /** 选中候选 Estimated 输出 Token 总数。 */
        long estimatedOutputTokens,
        /** 选中候选 Estimated 总 Token。 */
        long estimatedTotalTokens,
        /** Estimate 算法版本。 */
        String estimateMethod,
        /** 有序候选、Estimate 和预算决策指纹。 */
        String candidateFingerprint,
        /** Candidate Limit 内的全部有序候选决策。 */
        List<ResolvedPreviewCandidate> candidates) {

    public ResolvedAnalysisPreview {
        candidates = List.copyOf(candidates);
    }
}
