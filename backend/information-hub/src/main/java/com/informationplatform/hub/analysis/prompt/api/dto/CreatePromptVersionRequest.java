package com.informationplatform.hub.analysis.prompt.api.dto;

import jakarta.validation.constraints.NotBlank;

/** 创建或复用不可变 User Prompt Version 请求。 */
public record CreatePromptVersionRequest(
        /** User Prompt 原文，最大 8,000 个 Unicode 字符。 */
        @NotBlank String content) {
}
