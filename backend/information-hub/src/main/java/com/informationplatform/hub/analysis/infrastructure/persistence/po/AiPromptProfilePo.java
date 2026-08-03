package com.informationplatform.hub.analysis.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射账号维度的 AI Prompt 配置档案。 */
@TableName("ai_prompt_profile")
public class AiPromptProfilePo {

    /** Prompt 配置档案自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拥有该 Prompt 配置档案的用户账号主键。 */
    private Long userId;

    /** 用户可识别的 Prompt 配置档案名称。 */
    private String name;

    /** 平台定义的分析定义键，当前仅支持 JOB_USER_RELEVANCE。 */
    private String analysisDefinitionKey;

    /** 当前启用的 Prompt 版本主键，尚未发布版本时允许为空。 */
    private Long activeVersionId;

    /** 配置档案状态，例如 ACTIVE 或 DISABLED。 */
    private String status;

    /** 配置档案创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** 配置档案最近更新时间，按 UTC 保存。 */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnalysisDefinitionKey() {
        return analysisDefinitionKey;
    }

    public void setAnalysisDefinitionKey(String analysisDefinitionKey) {
        this.analysisDefinitionKey = analysisDefinitionKey;
    }

    public Long getActiveVersionId() {
        return activeVersionId;
    }

    public void setActiveVersionId(Long activeVersionId) {
        this.activeVersionId = activeVersionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
