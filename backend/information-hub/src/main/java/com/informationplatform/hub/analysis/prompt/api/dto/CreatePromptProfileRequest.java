package com.informationplatform.hub.analysis.prompt.api.dto;

import jakarta.validation.constraints.NotBlank;

/** 创建账号级 Prompt Profile 请求。 */
public record CreatePromptProfileRequest(
        /** 用户可识别的 Profile 名称，服务端会去除首尾空白。 */
        @NotBlank String name,
        /** 平台 Analysis Definition Key，TASK-022 仅允许 JOB_USER_RELEVANCE。 */
        @NotBlank String analysisDefinitionKey) {
}
