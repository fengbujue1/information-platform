package com.informationplatform.hub.analysis.preview.domain;

import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewRequestException;

/** Preview 默认值与冻结平台硬上限。 */
public record AnalysisPreviewLimits(
        /** 最近窗口天数。 */
        int windowDays,
        /** Candidate Limit。 */
        int maxCandidates,
        /** Estimated Total Token Budget。 */
        long maxEstimatedTokens) {

    public static final int DEFAULT_WINDOW_DAYS = 3;
    public static final int MAX_WINDOW_DAYS = 14;
    public static final int DEFAULT_MAX_CANDIDATES = 20;
    public static final int MAX_CANDIDATES = 50;
    public static final long DEFAULT_MAX_ESTIMATED_TOKENS = 75_000;
    public static final long MAX_ESTIMATED_TOKENS = 200_000;

    /** 应用默认值并拒绝非正数或超过冻结上限的请求。 */
    public static AnalysisPreviewLimits resolve(
            Integer windowDays, Integer maxCandidates, Long maxEstimatedTokens) {
        int resolvedWindow =
                windowDays == null ? DEFAULT_WINDOW_DAYS : windowDays;
        int resolvedCandidates =
                maxCandidates == null ? DEFAULT_MAX_CANDIDATES : maxCandidates;
        long resolvedTokens = maxEstimatedTokens == null
                ? DEFAULT_MAX_ESTIMATED_TOKENS
                : maxEstimatedTokens;
        if (resolvedWindow <= 0 || resolvedWindow > MAX_WINDOW_DAYS) {
            throw request(
                    "PREVIEW_WINDOW_DAYS_INVALID",
                    "windowDays must be between 1 and " + MAX_WINDOW_DAYS);
        }
        if (resolvedCandidates <= 0 || resolvedCandidates > MAX_CANDIDATES) {
            throw request(
                    "PREVIEW_MAX_CANDIDATES_INVALID",
                    "maxCandidates must be between 1 and " + MAX_CANDIDATES);
        }
        if (resolvedTokens <= 0 || resolvedTokens > MAX_ESTIMATED_TOKENS) {
            throw request(
                    "PREVIEW_MAX_ESTIMATED_TOKENS_INVALID",
                    "maxEstimatedTokens must be between 1 and " + MAX_ESTIMATED_TOKENS);
        }
        return new AnalysisPreviewLimits(resolvedWindow, resolvedCandidates, resolvedTokens);
    }

    private static AnalysisPreviewRequestException request(String code, String message) {
        return new AnalysisPreviewRequestException(code, message);
    }
}
