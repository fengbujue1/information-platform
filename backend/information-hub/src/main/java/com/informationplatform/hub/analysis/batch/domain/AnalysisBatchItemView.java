package com.informationplatform.hub.analysis.batch.domain;

import java.time.LocalDateTime;

/** Owner 安全的冻结 Batch Item 查询视图。 */
public record AnalysisBatchItemView(
        /** Batch Item 主键。 */
        long id,
        /** Information 主键。 */
        long informationId,
        /** 不可变 Snapshot 主键。 */
        long snapshotId,
        /** 创建或复用的 Analysis 主键。 */
        Long analysisId,
        /** 从 1 开始的稳定候选顺序。 */
        int selectionOrder,
        /** Item 状态。 */
        String status,
        /** 跳过或延后的稳定原因。 */
        String decisionReason,
        /** Estimated 输入 Token。 */
        Long estimatedInputTokens,
        /** Estimated 输出 Token。 */
        Long estimatedOutputTokens,
        /** Estimated 总 Token。 */
        Long estimatedTotalTokens,
        /** UTC 开始时间。 */
        LocalDateTime startedAt,
        /** UTC 完成时间。 */
        LocalDateTime completedAt) {
}
