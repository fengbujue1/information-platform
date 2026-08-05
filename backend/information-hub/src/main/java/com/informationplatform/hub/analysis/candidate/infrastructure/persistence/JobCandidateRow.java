package com.informationplatform.hub.analysis.candidate.infrastructure.persistence;

import java.time.LocalDateTime;

/** JOB 当前 Snapshot 的安全候选查询投影。 */
public class JobCandidateRow {

    /** Information 主键。 */
    private Long informationId;

    /** 当前 Snapshot 主键。 */
    private Long snapshotId;

    /** 信息类型，当前固定为 JOB。 */
    private String informationType;

    /** Information 首次入库 UTC 时间。 */
    private LocalDateTime firstSeenTime;

    /** Snapshot 标题。 */
    private String title;

    /** Snapshot 正文。 */
    private String content;

    /** Snapshot 标准化 JSON，不包含 rawPayload。 */
    private String standardizedPayload;

    public Long getInformationId() {
        return informationId;
    }

    public void setInformationId(Long informationId) {
        this.informationId = informationId;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getInformationType() {
        return informationType;
    }

    public void setInformationType(String informationType) {
        this.informationType = informationType;
    }

    public LocalDateTime getFirstSeenTime() {
        return firstSeenTime;
    }

    public void setFirstSeenTime(LocalDateTime firstSeenTime) {
        this.firstSeenTime = firstSeenTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStandardizedPayload() {
        return standardizedPayload;
    }

    public void setStandardizedPayload(String standardizedPayload) {
        this.standardizedPayload = standardizedPayload;
    }
}
