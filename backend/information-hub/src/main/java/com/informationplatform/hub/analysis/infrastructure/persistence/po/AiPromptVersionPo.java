package com.informationplatform.hub.analysis.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射不可变的用户 Prompt 历史版本。 */
@TableName("ai_prompt_version")
public class AiPromptVersionPo {

    /** Prompt 版本自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 Prompt 配置档案主键。 */
    private Long promptProfileId;

    /** 配置档案内单调递增的版本号，从 1 开始。 */
    private Integer versionNo;

    /** 用户维护的 Prompt 文本，不包含平台控制的 System Prompt 和 Schema。 */
    private String content;

    /** Prompt 文本的 SHA-256 哈希值，用于内容比对。 */
    private String contentHash;

    /** Prompt 版本创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPromptProfileId() {
        return promptProfileId;
    }

    public void setPromptProfileId(Long promptProfileId) {
        this.promptProfileId = promptProfileId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
