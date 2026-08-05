package com.informationplatform.hub.analysis.preview.domain;

import java.time.Instant;

/** HMAC 签名但不加密的 Preview 冻结载荷。 */
public record PreviewTokenPayload(
        /** Token 格式版本。 */
        int tokenVersion,
        /** 当前 Owner 主键。 */
        long userId,
        /** Prompt Profile 主键。 */
        long promptProfileId,
        /** 不可变 Prompt Version 主键。 */
        long promptVersionId,
        /** Definition Key。 */
        String definitionKey,
        /** Definition 版本。 */
        int definitionVersion,
        /** UTC 半开窗口起点。 */
        Instant windowStart,
        /** UTC 半开窗口终点。 */
        Instant windowEnd,
        /** 请求窗口天数。 */
        int windowDays,
        /** 请求 Candidate Limit。 */
        int maxCandidates,
        /** 请求 Token Budget。 */
        long maxEstimatedTokens,
        /** 窗口内总数。 */
        long totalInWindow,
        /** 未成功分析数量。 */
        long eligibleCount,
        /** 已成功分析数量。 */
        long alreadyAnalyzedCount,
        /** 最终选中数量。 */
        long selectedCount,
        /** Item Limit 延后数量。 */
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
        /** 有序候选、Estimate 与决策 SHA-256。 */
        String candidateFingerprint,
        /** Manual Confirm 幂等随机标识。 */
        String manualRequestId,
        /** 签发 UTC 时间。 */
        Instant issuedAt,
        /** 到期 UTC 时间。 */
        Instant expiresAt) {
}
