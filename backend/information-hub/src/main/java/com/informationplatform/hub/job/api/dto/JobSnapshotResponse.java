package com.informationplatform.hub.job.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** 职位历史快照响应，明确排除快照中的 rawPayload。 */
public record JobSnapshotResponse(
        /** 快照主键。 */
        long id,
        /** 该快照对应的业务版本号。 */
        int versionNo,
        /** 该版本标准化业务内容的 SHA-256 哈希值。 */
        String contentHash,
        /** 该版本的职位标题。 */
        String title,
        /** 该版本的职位正文。 */
        String content,
        /** 该版本用于追溯的标准化业务 JSON。 */
        JsonNode standardizedPayload,
        /** 产生该版本的数据采集时间，UTC。 */
        Instant collectedAt,
        /** 产生该版本的采集器标识。 */
        String collectorId,
        /** 产生该版本的采集器版本。 */
        String collectorVersion,
        /** 快照记录创建时间，UTC。 */
        Instant createdAt) {}
