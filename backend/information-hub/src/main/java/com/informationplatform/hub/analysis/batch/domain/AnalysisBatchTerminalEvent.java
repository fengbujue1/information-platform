package com.informationplatform.hub.analysis.batch.domain;

/** Analysis Batch 已在当前事务中进入终态的进程内事件。 */
public record AnalysisBatchTerminalEvent(
        /** Analysis Batch 主键。 */ long batchId,
        /** 已持久化的终态：COMPLETED、PARTIAL_FAILED、FAILED 或 NOOP。 */ String status) {

    public AnalysisBatchTerminalEvent {
        if (batchId <= 0 || status == null || status.isBlank()) {
            throw new IllegalArgumentException("Analysis Batch terminal event is incomplete");
        }
    }
}
