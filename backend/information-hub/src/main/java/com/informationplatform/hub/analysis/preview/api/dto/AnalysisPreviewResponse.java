package com.informationplatform.hub.analysis.preview.api.dto;

import com.informationplatform.hub.analysis.preview.domain.AnalysisPreview;
import java.time.Instant;

/** Manual Confirm 可安全复用的 Preview 汇总响应。 */
public record AnalysisPreviewResponse(
        /** UTC 半开窗口起点。 */
        Instant windowStart,
        /** UTC 半开窗口终点。 */
        Instant windowEnd,
        /** 窗口内总数。 */
        long totalInWindow,
        /** 未成功分析数量。 */
        long eligibleCount,
        /** 待分析数量，等于 eligibleCount。 */
        long pendingCount,
        /** 已成功分析数量。 */
        long alreadyAnalyzedCount,
        /** Candidate/Token Limit 后选中数量。 */
        long selectedCount,
        /** Candidate Limit 延后数量。 */
        long deferredByItemLimitCount,
        /** Token Budget 延后数量。 */
        long deferredByTokenBudgetCount,
        /** 选中候选 Estimated 输入 Token。 */
        long estimatedInputTokens,
        /** 选中候选 Estimated 输出 Token。 */
        long estimatedOutputTokens,
        /** 选中候选 Estimated 总 Token。 */
        long estimatedTotalTokens,
        /** Estimate 算法版本。 */
        String estimateMethod,
        /** Preview Token 到期 UTC 时间。 */
        Instant expiresAt,
        /** HMAC Preview Token。 */
        String previewToken) {

    public static AnalysisPreviewResponse from(AnalysisPreview preview) {
        return new AnalysisPreviewResponse(
                preview.windowStart(),
                preview.windowEnd(),
                preview.totalInWindow(),
                preview.eligibleCount(),
                preview.pendingCount(),
                preview.alreadyAnalyzedCount(),
                preview.selectedCount(),
                preview.deferredByItemLimitCount(),
                preview.deferredByTokenBudgetCount(),
                preview.estimatedInputTokens(),
                preview.estimatedOutputTokens(),
                preview.estimatedTotalTokens(),
                preview.estimateMethod(),
                preview.expiresAt(),
                preview.previewToken());
    }
}
