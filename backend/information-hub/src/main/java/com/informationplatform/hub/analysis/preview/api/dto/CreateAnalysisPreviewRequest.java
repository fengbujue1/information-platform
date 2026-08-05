package com.informationplatform.hub.analysis.preview.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 创建无副作用 Manual Analysis Preview 的请求。 */
public record CreateAnalysisPreviewRequest(
        /** 当前 Owner 的 Prompt Profile 主键。 */
        @NotNull @Positive Long promptProfileId,
        /** 最近窗口天数；为空使用 3。 */
        @Positive Integer windowDays,
        /** Candidate Limit；为空使用 20。 */
        @Positive Integer maxCandidates,
        /** Estimated Total Token Budget；为空使用 75000。 */
        @Positive Long maxEstimatedTokens) {
}
