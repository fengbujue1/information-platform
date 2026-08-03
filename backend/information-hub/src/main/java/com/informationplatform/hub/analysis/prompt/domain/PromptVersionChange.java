package com.informationplatform.hub.analysis.prompt.domain;

/** 创建新 Version 或复用已有 Version 后的结果。 */
public record PromptVersionChange(
        /** 已创建或复用并切换为 Active 的 Prompt Version。 */
        PromptVersion version,
        /** true 表示新建，false 表示按相同 contentHash 复用。 */
        boolean created) {
}
