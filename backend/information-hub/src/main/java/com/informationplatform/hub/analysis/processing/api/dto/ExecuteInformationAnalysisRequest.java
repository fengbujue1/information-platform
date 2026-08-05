package com.informationplatform.hub.analysis.processing.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 创建或复用单条 Analysis 的请求。 */
public record ExecuteInformationAnalysisRequest(
        /** Information 主键。 */
        @NotNull @Positive Long informationId,
        /** 明确历史 Snapshot 主键；为空时解析 Information 当前 Snapshot。 */
        @Positive Long snapshotId,
        /** 当前 Owner 的 Prompt Profile 主键。 */
        @NotNull @Positive Long promptProfileId,
        /** 是否显式重试同一逻辑身份的 FAILED Analysis。 */
        Boolean retryFailed) {

    public boolean shouldRetryFailed() {
        return Boolean.TRUE.equals(retryFailed);
    }
}
