package com.informationplatform.hub.ingestion.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record InformationEnvelopeRequest(
        /** 统一采集协议的版本号。 */
        @NotNull Integer schemaVersion,
        /** 信息类型，第一阶段固定为 JOB。 */
        @NotBlank @Size(max = 32) String informationType,
        /** 信息来源，第一阶段固定为 BOSS。 */
        @NotBlank @Size(max = 64) String source,
        /** 来源系统中的信息唯一标识。 */
        @NotBlank @Size(max = 255) String sourceItemId,
        /** 来源信息详情页地址。 */
        @Size(max = 2048) String sourceUrl,
        /** 信息标题。 */
        @NotBlank @Size(max = 1000) String title,
        /** 信息正文。 */
        String content,
        /** 来源系统标记的发布时间。 */
        OffsetDateTime publishTime,
        /** 采集器实际采集到该信息的时间。 */
        @NotNull OffsetDateTime collectedAt,
        /** 本次请求的采集器元数据。 */
        @NotNull @Valid CollectorRequest collector,
        /** 搜索条件、页码等非业务采集上下文。 */
        JsonNode collectionContext,
        /** 信息类型对应的业务扩展数据。 */
        @NotNull @Valid JobExtensionRequest extension,
        /** 采集器获得的完整原始数据。 */
        @NotNull JsonNode rawPayload) {}
