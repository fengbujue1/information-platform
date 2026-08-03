package com.informationplatform.hub.analysis.prompt.domain;

import java.time.LocalDateTime;

/** 不可变 User Prompt 的历史版本。 */
public record PromptVersion(
        /** Prompt Version 主键。 */
        long id,
        /** 所属 Prompt Profile 主键。 */
        long promptProfileId,
        /** Profile 内从 1 开始递增的版本号。 */
        int versionNo,
        /** 用户维护的 Prompt 原文。 */
        String content,
        /** Prompt 原文 UTF-8 字节的 SHA-256 十六进制摘要。 */
        String contentHash,
        /** Version 创建时间，数据库按 UTC 保存。 */
        LocalDateTime createdAt) {
}
