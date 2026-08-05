package com.informationplatform.hub.analysis.preview.domain;

import java.time.LocalDateTime;

/** 指纹中冻结的有序候选、Estimate 与预算决策。 */
public record PreviewCandidateEstimate(
        /** Information 主键。 */
        long informationId,
        /** 当前 Snapshot 主键。 */
        long snapshotId,
        /** FIRST_INGESTED UTC 时间。 */
        LocalDateTime firstSeenTime,
        /** Estimated 输入 Token。 */
        long estimatedInputTokens,
        /** Estimated 输出 Token。 */
        long estimatedOutputTokens,
        /** Estimated 总 Token。 */
        long estimatedTotalTokens,
        /** 是否进入本次预算选择。 */
        boolean selected) {

    public PreviewCandidateEstimate {
        if (informationId <= 0
                || snapshotId <= 0
                || firstSeenTime == null
                || estimatedInputTokens < 0
                || estimatedOutputTokens < 0
                || estimatedTotalTokens != estimatedInputTokens + estimatedOutputTokens) {
            throw new IllegalArgumentException("Preview candidate estimate is invalid");
        }
    }
}
