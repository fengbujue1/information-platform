package com.informationplatform.hub.analysis.candidate.infrastructure.persistence;

/** Candidate 窗口聚合计数投影。 */
public class CandidateWindowCountsRow {

    /** 窗口内总数。 */
    private Long totalInWindow;

    /** 已存在相同成功逻辑身份的数量。 */
    private Long alreadyAnalyzedCount;

    public Long getTotalInWindow() {
        return totalInWindow;
    }

    public void setTotalInWindow(Long totalInWindow) {
        this.totalInWindow = totalInWindow;
    }

    public Long getAlreadyAnalyzedCount() {
        return alreadyAnalyzedCount;
    }

    public void setAlreadyAnalyzedCount(Long alreadyAnalyzedCount) {
        this.alreadyAnalyzedCount = alreadyAnalyzedCount;
    }
}
