package com.informationplatform.hub.ingestion.domain;

public record ArchiveState(
        /** 平台内部的信息主键。 */
        long informationId,
        /** 数据库中当前保存的版本号。 */
        int currentVersionNo,
        /** 当前标准化业务内容的哈希值。 */
        String contentHash,
        /** 当前已归档的完整业务内容。 */
        ArchiveContent content) {}
