package com.informationplatform.hub.analysis.prompt.api.dto;

import jakarta.validation.constraints.NotBlank;

/** 停用或重新启用 Prompt Profile 请求。 */
public record UpdatePromptProfileStatusRequest(
        /** Profile 状态，只允许 ACTIVE 或 DISABLED。 */
        @NotBlank String status) {
}
