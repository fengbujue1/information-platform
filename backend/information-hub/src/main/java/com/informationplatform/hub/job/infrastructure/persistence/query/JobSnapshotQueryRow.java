package com.informationplatform.hub.job.infrastructure.persistence.query;

import java.time.LocalDateTime;

/** 职位快照查询结果，不包含快照 rawPayload。 */
public class JobSnapshotQueryRow {

    /** 快照主键。 */
    private Long id;

    /** 该快照对应的业务版本号。 */
    private Integer versionNo;

    /** 该版本标准化业务内容的 SHA-256 哈希值。 */
    private String contentHash;

    /** 该版本的职位标题。 */
    private String title;

    /** 该版本的职位正文。 */
    private String content;

    /** 该版本用于追溯的标准化业务 JSON 文本。 */
    private String standardizedPayload;

    /** 产生该版本的数据采集时间。 */
    private LocalDateTime collectedAt;

    /** 产生该版本的采集器标识。 */
    private String collectorId;

    /** 产生该版本的采集器版本。 */
    private String collectorVersion;

    /** 快照记录创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
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

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public String getCollectorId() {
        return collectorId;
    }

    public void setCollectorId(String collectorId) {
        this.collectorId = collectorId;
    }

    public String getCollectorVersion() {
        return collectorVersion;
    }

    public void setCollectorVersion(String collectorVersion) {
        this.collectorVersion = collectorVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
