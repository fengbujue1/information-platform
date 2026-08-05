package com.informationplatform.hub.analysis.batch.api.dto;

import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchItemView;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Batch Detail 中的冻结 Item。 */
public record AnalysisBatchItemResponse(
        /** Batch Item 主键。 */ long id,
        /** Information 主键。 */ long informationId,
        /** Snapshot 主键。 */ long snapshotId,
        /** Analysis 主键。 */ Long analysisId,
        /** 稳定选择顺序。 */ int selectionOrder,
        /** Item 状态。 */ String status,
        /** 跳过或延后原因。 */ String decisionReason,
        /** Estimated 输入 Token。 */ Long estimatedInputTokens,
        /** Estimated 输出 Token。 */ Long estimatedOutputTokens,
        /** Estimated 总 Token。 */ Long estimatedTotalTokens,
        /** UTC 开始时间。 */ Instant startedAt,
        /** UTC 完成时间。 */ Instant completedAt) {

    public static AnalysisBatchItemResponse from(AnalysisBatchItemView item) {
        return new AnalysisBatchItemResponse(
                item.id(),
                item.informationId(),
                item.snapshotId(),
                item.analysisId(),
                item.selectionOrder(),
                item.status(),
                item.decisionReason(),
                item.estimatedInputTokens(),
                item.estimatedOutputTokens(),
                item.estimatedTotalTokens(),
                toInstant(item.startedAt()),
                toInstant(item.completedAt()));
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
