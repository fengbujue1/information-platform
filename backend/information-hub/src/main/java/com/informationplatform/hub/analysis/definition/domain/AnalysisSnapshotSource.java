package com.informationplatform.hub.analysis.definition.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record AnalysisSnapshotSource(
        /** 不可变信息快照 ID。 */
        long snapshotId,
        /** 快照所属信息 ID。 */
        long informationId,
        /** 快照对应的信息类型。 */
        AnalysisInformationType informationType,
        /** 快照冻结的标题。 */
        String title,
        /** 快照冻结的正文。 */
        String content,
        /** 快照冻结的标准化业务数据，不包含原始 payload。 */
        JsonNode standardizedPayload) {

    public AnalysisSnapshotSource {
        if (snapshotId <= 0 || informationId <= 0) {
            throw new IllegalArgumentException("Snapshot and information ids must be positive");
        }
        if (informationType == null) {
            throw new IllegalArgumentException("Information type must not be null");
        }
        if (standardizedPayload == null) {
            throw new IllegalArgumentException("Standardized payload must not be null");
        }
        standardizedPayload = standardizedPayload.deepCopy();
    }

    @Override
    public JsonNode standardizedPayload() {
        return standardizedPayload.deepCopy();
    }
}
