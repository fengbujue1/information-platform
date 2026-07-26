package com.informationplatform.hub.ingestion.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record InformationFields(
        /** 统一采集协议的版本号。 */
        int schemaVersion,
        /** 信息类型。 */
        String informationType,
        /** 信息来源。 */
        String source,
        /** 来源系统中的信息唯一标识。 */
        String sourceItemId,
        /** 来源信息详情页地址。 */
        String sourceUrl,
        /** 信息标题。 */
        String title,
        /** 信息正文。 */
        String content,
        /** 来源系统标记的发布时间。 */
        Instant publishTime,
        /** 采集器实际采集到该信息的时间。 */
        Instant collectedAt,
        /** 采集器实例或实现的稳定标识。 */
        String collectorId,
        /** 本次采集使用的采集器版本。 */
        String collectorVersion,
        /** 搜索条件、页码等非业务采集上下文。 */
        JsonNode collectionContext,
        /** 采集器获得的完整原始数据。 */
        JsonNode rawPayload) {}
