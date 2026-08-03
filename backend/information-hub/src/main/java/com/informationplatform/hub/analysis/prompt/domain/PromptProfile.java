package com.informationplatform.hub.analysis.prompt.domain;

import java.time.LocalDateTime;

/** 当前账号可见的 Prompt Profile 领域投影。 */
public record PromptProfile(
        /** Prompt Profile 主键。 */
        long id,
        /** 用户可识别的 Profile 名称。 */
        String name,
        /** 平台控制的 Analysis Definition Key。 */
        String analysisDefinitionKey,
        /** 当前启用的 Prompt Version 主键，尚未创建版本时为空。 */
        Long activeVersionId,
        /** Profile 状态，只允许 ACTIVE 或 DISABLED。 */
        PromptProfileStatus status,
        /** Profile 创建时间，数据库按 UTC 保存。 */
        LocalDateTime createdAt,
        /** Profile 最近更新时间，数据库按 UTC 保存。 */
        LocalDateTime updatedAt) {
}
