package com.informationplatform.hub.analysis.candidate.domain;

import com.informationplatform.hub.analysis.definition.domain.AnalysisSnapshotSource;
import java.time.LocalDateTime;

/** 一个按稳定顺序解析出的当前 Snapshot 候选。 */
public record AnalysisCandidate(
        /** Information 主键。 */
        long informationId,
        /** 当前不可变 Snapshot 主键。 */
        long snapshotId,
        /** Information 首次入库 UTC 时间。 */
        LocalDateTime firstSeenTime,
        /** 不含 rawPayload 的安全 Snapshot 输入。 */
        AnalysisSnapshotSource snapshotSource) {

    public AnalysisCandidate {
        if (informationId <= 0
                || snapshotId <= 0
                || firstSeenTime == null
                || snapshotSource == null
                || snapshotSource.informationId() != informationId
                || snapshotSource.snapshotId() != snapshotId) {
            throw new IllegalArgumentException("Analysis candidate is invalid");
        }
    }
}
