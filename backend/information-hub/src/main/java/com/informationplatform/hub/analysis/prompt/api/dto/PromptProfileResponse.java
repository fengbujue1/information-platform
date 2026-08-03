package com.informationplatform.hub.analysis.prompt.api.dto;

import com.informationplatform.hub.analysis.prompt.domain.PromptProfile;
import com.informationplatform.hub.analysis.prompt.domain.PromptProfileStatus;
import java.time.Instant;
import java.time.ZoneOffset;

/** Prompt Profile 的 Owner 安全 API 投影。 */
public record PromptProfileResponse(
        /** Prompt Profile 主键。 */
        long id,
        /** 用户可识别的 Profile 名称。 */
        String name,
        /** 平台控制的 Analysis Definition Key。 */
        String analysisDefinitionKey,
        /** 当前 Active Version 主键，尚无版本时为空。 */
        Long activeVersionId,
        /** Profile 状态。 */
        PromptProfileStatus status,
        /** Profile 创建时间，UTC。 */
        Instant createdAt,
        /** Profile 最近更新时间，UTC。 */
        Instant updatedAt) {

    /** 将内部领域投影转换为不包含 Owner 主键的 API 响应。 */
    public static PromptProfileResponse from(PromptProfile profile) {
        return new PromptProfileResponse(
                profile.id(),
                profile.name(),
                profile.analysisDefinitionKey(),
                profile.activeVersionId(),
                profile.status(),
                profile.createdAt().toInstant(ZoneOffset.UTC),
                profile.updatedAt().toInstant(ZoneOffset.UTC));
    }
}
