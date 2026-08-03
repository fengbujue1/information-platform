package com.informationplatform.hub.analysis.prompt.api.dto;

import com.informationplatform.hub.analysis.prompt.domain.PromptVersion;
import java.time.Instant;
import java.time.ZoneOffset;

/** 不可变 Prompt Version API 投影。 */
public record PromptVersionResponse(
        /** Prompt Version 主键。 */
        long id,
        /** 所属 Prompt Profile 主键。 */
        long promptProfileId,
        /** Profile 内从 1 开始递增的版本号。 */
        int versionNo,
        /** User Prompt 原文。 */
        String content,
        /** Prompt 原文 UTF-8 SHA-256 摘要。 */
        String contentHash,
        /** Version 创建时间，UTC。 */
        Instant createdAt) {

    public static PromptVersionResponse from(PromptVersion version) {
        return new PromptVersionResponse(
                version.id(),
                version.promptProfileId(),
                version.versionNo(),
                version.content(),
                version.contentHash(),
                version.createdAt().toInstant(ZoneOffset.UTC));
    }
}
