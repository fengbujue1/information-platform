package com.informationplatform.hub.analysis.preview.api.dto;

import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewLimitPolicy.AnalysisLimitRange;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewLimitPolicy.AnalysisPreviewLimitMetadata;

/** 浏览器可读取的分析默认值和平台限制，不包含任何服务端秘密。 */
public record AnalysisPreviewLimitsResponse(
        /** 候选窗口天数限制。 */
        AnalysisLimitRangeResponse windowDays,
        /** 最大候选数量限制。 */
        AnalysisLimitRangeResponse maxCandidates,
        /** 预估 Token 预算限制。 */
        AnalysisLimitRangeResponse maxEstimatedTokens) {

    public static AnalysisPreviewLimitsResponse from(AnalysisPreviewLimitMetadata metadata) {
        return new AnalysisPreviewLimitsResponse(
                AnalysisLimitRangeResponse.from(metadata.windowDays()),
                AnalysisLimitRangeResponse.from(metadata.maxCandidates()),
                AnalysisLimitRangeResponse.from(metadata.maxEstimatedTokens()));
    }

    /** 单个限制项的当前默认值、最小值和平台上限。 */
    public record AnalysisLimitRangeResponse(
            /** 当前默认值。 */
            long defaultValue,
            /** 当前最小值。 */
            long minimum,
            /** 当前平台上限。 */
            long maximum) {

        private static AnalysisLimitRangeResponse from(AnalysisLimitRange range) {
            return new AnalysisLimitRangeResponse(
                    range.defaultValue(), range.minimum(), range.maximum());
        }
    }
}
