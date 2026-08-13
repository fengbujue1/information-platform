package com.informationplatform.hub.analysis.preview.domain;

/** 单次分析预览已经解析并验证的平台限制。 */
public record AnalysisPreviewLimits(
        /** 最近窗口天数。 */
        int windowDays,
        /** 最大候选数量。 */
        int maxCandidates,
        /** 预估 Token 总预算。 */
        long maxEstimatedTokens) {}
