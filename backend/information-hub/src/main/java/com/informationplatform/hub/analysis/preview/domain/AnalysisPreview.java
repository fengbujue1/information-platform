package com.informationplatform.hub.analysis.preview.domain;

import java.time.Instant;

/** 无副作用 Preview 的完整 Owner 安全结果。 */
public record AnalysisPreview(
        /** UTC 半开窗口起点。 */
        Instant windowStart,
        /** UTC 半开窗口终点。 */
        Instant windowEnd,
        /** 窗口内总数。 */
        long totalInWindow,
        /** 未成功分析数量。 */
        long eligibleCount,
        /** 未成功分析数量的显式别名。 */
        long pendingCount,
        /** 已成功分析数量。 */
        long alreadyAnalyzedCount,
        /** Candidate/Token Limit 后选中数量。 */
        long selectedCount,
        /** Candidate Limit 延后数量。 */
        long deferredByItemLimitCount,
        /** Token Budget 延后数量。 */
        long deferredByTokenBudgetCount,
        /** 选中候选 Estimated 输入 Token 总计。 */
        long estimatedInputTokens,
        /** 选中候选 Estimated 输出 Token 总计。 */
        long estimatedOutputTokens,
        /** 选中候选 Estimated 总 Token。 */
        long estimatedTotalTokens,
        /** Estimate 算法版本。 */
        String estimateMethod,
        /** Token 到期 UTC 时间。 */
        Instant expiresAt,
        /** 10 分钟有效的 HMAC Preview Token。 */
        String previewToken) {
}
