package com.informationplatform.hub.analysis.preview.application;

import com.informationplatform.hub.analysis.preview.domain.AnalysisPreviewLimits;
import com.informationplatform.hub.analysis.preview.infrastructure.config.AnalysisPreviewProperties;
import org.springframework.stereotype.Component;

/** 统一应用手动分析和定时分析的服务端默认值与平台上限。 */
@Component
public class AnalysisPreviewLimitPolicy {

    /** 所有浏览器和调度入口共享的启动配置。 */
    private final AnalysisPreviewProperties properties;

    public AnalysisPreviewLimitPolicy(AnalysisPreviewProperties properties) {
        this.properties = properties;
    }

    /** 应用当前默认值并拒绝非正数或超过当前平台上限的请求。 */
    public AnalysisPreviewLimits resolve(
            Integer windowDays, Integer maxCandidates, Long maxEstimatedTokens) {
        int resolvedWindow = windowDays == null
                ? properties.getDefaultWindowDays()
                : windowDays;
        int resolvedCandidates = maxCandidates == null
                ? properties.getDefaultMaxCandidates()
                : maxCandidates;
        long resolvedTokens = maxEstimatedTokens == null
                ? properties.getDefaultMaxEstimatedTokens()
                : maxEstimatedTokens;
        requireWithin(
                resolvedWindow,
                properties.getMaxWindowDays(),
                "PREVIEW_WINDOW_DAYS_INVALID",
                "windowDays");
        requireWithin(
                resolvedCandidates,
                properties.getMaxCandidates(),
                "PREVIEW_MAX_CANDIDATES_INVALID",
                "maxCandidates");
        requireWithin(
                resolvedTokens,
                properties.getMaxEstimatedTokens(),
                "PREVIEW_MAX_ESTIMATED_TOKENS_INVALID",
                "maxEstimatedTokens");
        return new AnalysisPreviewLimits(resolvedWindow, resolvedCandidates, resolvedTokens);
    }

    /** 返回允许浏览器展示的非敏感限制元数据。 */
    public AnalysisPreviewLimitMetadata metadata() {
        return new AnalysisPreviewLimitMetadata(
                new AnalysisLimitRange(
                        properties.getDefaultWindowDays(), 1, properties.getMaxWindowDays()),
                new AnalysisLimitRange(
                        properties.getDefaultMaxCandidates(), 1, properties.getMaxCandidates()),
                new AnalysisLimitRange(
                        properties.getDefaultMaxEstimatedTokens(),
                        1,
                        properties.getMaxEstimatedTokens()));
    }

    private void requireWithin(long value, long maximum, String code, String field) {
        if (value <= 0 || value > maximum) {
            throw new AnalysisPreviewRequestException(
                    code, field + " must be between 1 and " + maximum);
        }
    }

    /** 单个限制项可公开的默认值、最小值和当前平台上限。 */
    public record AnalysisLimitRange(
            /** 当前默认值。 */
            long defaultValue,
            /** 当前最小值。 */
            long minimum,
            /** 当前平台上限。 */
            long maximum) {}

    /** 分析入口共享的三组非敏感限制元数据。 */
    public record AnalysisPreviewLimitMetadata(
            /** 候选窗口天数限制。 */
            AnalysisLimitRange windowDays,
            /** 最大候选数量限制。 */
            AnalysisLimitRange maxCandidates,
            /** 预估 Token 预算限制。 */
            AnalysisLimitRange maxEstimatedTokens) {}
}
