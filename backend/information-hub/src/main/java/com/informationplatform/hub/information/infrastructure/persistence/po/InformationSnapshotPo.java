package com.informationplatform.hub.information.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("information_snapshot")
public class InformationSnapshotPo {

    /** 信息快照表自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快照所属的信息主键。 */
    private Long informationId;

    /** 该快照对应的信息版本号。 */
    private Integer versionNo;

    /** 该版本标准化业务内容的 SHA-256 哈希值。 */
    private String contentHash;

    /** 该版本的信息标题。 */
    private String title;

    /** 该版本的信息正文。 */
    private String content;

    /** 该版本用于比较和追溯的标准化业务 JSON。 */
    private String standardizedPayload;

    /** 产生该版本时收到的完整原始 JSON 数据。 */
    private String rawPayload;

    /** 产生该版本的数据采集时间。 */
    private LocalDateTime collectedAt;

    /** 产生该版本的采集器标识。 */
    private String collectorId;

    /** 产生该版本的采集器版本。 */
    private String collectorVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInformationId() {
        return informationId;
    }

    public void setInformationId(Long informationId) {
        this.informationId = informationId;
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

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
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
}
