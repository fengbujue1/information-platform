package com.informationplatform.hub.analysis.batch.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Manual Preview 确认请求。 */
public record ConfirmAnalysisBatchRequest(
        /** Preview 返回的短期 HMAC Token。 */
        @NotBlank @Size(max = 32768) String previewToken) {
}
