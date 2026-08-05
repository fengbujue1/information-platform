package com.informationplatform.hub.analysis.infrastructure.persistence.query;

/** 仅供 Analysis 使用的安全 Snapshot 投影，不包含 raw_payload。 */
public class AnalysisSnapshotRow {

    /** Snapshot 主键。 */
    private Long snapshotId;

    /** Snapshot 所属 Information 主键。 */
    private Long informationId;

    /** Information 类型。 */
    private String informationType;

    /** Snapshot 冻结标题。 */
    private String title;

    /** Snapshot 冻结正文。 */
    private String content;

    /** Snapshot 冻结标准化 JSON，不包含原始 payload。 */
    private String standardizedPayload;

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Long getInformationId() {
        return informationId;
    }

    public void setInformationId(Long informationId) {
        this.informationId = informationId;
    }

    public String getInformationType() {
        return informationType;
    }

    public void setInformationType(String informationType) {
        this.informationType = informationType;
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
