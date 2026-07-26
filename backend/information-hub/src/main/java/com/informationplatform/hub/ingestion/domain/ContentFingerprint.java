package com.informationplatform.hub.ingestion.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record ContentFingerprint(
        /** 标准化业务内容的 SHA-256 哈希值。 */
        String hash,
        /** 创建版本快照时保存的标准化业务数据。 */
        JsonNode standardizedPayload) {}
