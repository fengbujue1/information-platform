package com.informationplatform.hub.information.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("information_item")
public class InformationItemPo {

    /** 信息主表自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 信息类型，例如 JOB。 */
    private String informationType;

    /** 信息来源，例如 BOSS。 */
    private String source;

    /** 来源系统中的信息唯一标识。 */
    private String sourceItemId;

    /** 来源信息详情页地址。 */
    private String sourceUrl;

    /** 信息标题。 */
    private String title;

    /** 信息正文。 */
    private String content;

    /** 来源系统标记的发布时间。 */
    private LocalDateTime publishTime;

    /** 采集器实际采集到该信息的时间。 */
    private LocalDateTime collectedAt;

    /** 平台首次发现该信息的服务端时间。 */
    private LocalDateTime firstSeenTime;

    /** 平台最近接收到该信息的服务端时间。 */
    private LocalDateTime lastSeenTime;

    /** 当前标准化业务内容的 SHA-256 哈希值。 */
    private String contentHash;

    /** 当前标准化业务内容的版本号。 */
    private Integer currentVersionNo;

    /** 最近一次上报的完整原始 JSON 数据。 */
    private String rawPayload;

    /** 统一采集协议的版本号。 */
    private Integer schemaVersion;

    /** 采集器实例或实现的稳定标识。 */
    private String collectorId;

    /** 最近一次上报所使用的采集器版本。 */
    private String collectorVersion;

    /** 搜索条件、页码等非业务采集上下文 JSON。 */
    private String collectionContext;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInformationType() {
        return informationType;
    }

    public void setInformationType(String informationType) {
        this.informationType = informationType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceItemId() {
        return sourceItemId;
    }

    public void setSourceItemId(String sourceItemId) {
        this.sourceItemId = sourceItemId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
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

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public LocalDateTime getFirstSeenTime() {
        return firstSeenTime;
    }

    public void setFirstSeenTime(LocalDateTime firstSeenTime) {
        this.firstSeenTime = firstSeenTime;
    }

    public LocalDateTime getLastSeenTime() {
        return lastSeenTime;
    }

    public void setLastSeenTime(LocalDateTime lastSeenTime) {
        this.lastSeenTime = lastSeenTime;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public Integer getCurrentVersionNo() {
        return currentVersionNo;
    }

    public void setCurrentVersionNo(Integer currentVersionNo) {
        this.currentVersionNo = currentVersionNo;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
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

    public String getCollectionContext() {
        return collectionContext;
    }

    public void setCollectionContext(String collectionContext) {
        this.collectionContext = collectionContext;
    }
}
