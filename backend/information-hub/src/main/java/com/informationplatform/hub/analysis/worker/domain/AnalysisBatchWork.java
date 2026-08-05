package com.informationplatform.hub.analysis.worker.domain;

import com.informationplatform.hub.analysis.processing.domain.AnalysisPreparation;

/** Worker 领取事务提交后可在事务外执行的单个 Batch Item。 */
public record AnalysisBatchWork(
        /** Batch 主键。 */
        long batchId,
        /** Batch Item 主键。 */
        long batchItemId,
        /** 新建 Invocation 的 Analysis 准备结果。 */
        AnalysisPreparation preparation) {

    public AnalysisBatchWork {
        if (batchId <= 0 || batchItemId <= 0 || preparation == null
                || preparation.executionPlan() == null) {
            throw new IllegalArgumentException("Analysis Batch work is invalid");
        }
    }
}
