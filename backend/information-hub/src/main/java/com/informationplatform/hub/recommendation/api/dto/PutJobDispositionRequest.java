package com.informationplatform.hub.recommendation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** 完整替换当前 JOB disposition 的请求。 */
public record PutJobDispositionRequest(
        /** JOB current state：NONE、CONTACTED 或 CONTACTED_NOT_SUITABLE。 */
        @NotBlank String jobDisposition,
        /** 触发本次行为的 Recommendation Item 主键；不归因时可为空。 */
        @Positive Long recommendationItemId) {
}
