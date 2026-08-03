package com.informationplatform.hub.analysis.prompt.api.dto;

import jakarta.validation.constraints.Positive;

/** 切换 Profile 当前 Active Version 请求。 */
public record ActivatePromptVersionRequest(
        /** 必须属于当前 Owner 和目标 Profile 的 Prompt Version 主键。 */
        @Positive long versionId) {
}
