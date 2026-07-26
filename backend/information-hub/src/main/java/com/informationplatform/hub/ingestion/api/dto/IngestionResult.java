package com.informationplatform.hub.ingestion.api.dto;

public record IngestionResult(
        /** 平台内部的信息主键。 */
        long informationId,
        /** 本次请求是否创建了新信息。 */
        boolean created,
        /** 本次请求是否改变了标准化业务内容。 */
        boolean contentChanged,
        /** 当前信息版本号。 */
        int versionNo,
        /** 本次请求是否创建了不可变快照。 */
        boolean snapshotCreated) {}
