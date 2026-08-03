package com.informationplatform.hub.analysis.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射绑定到固定信息快照的 AI 分析逻辑结果。 */
@TableName("information_analysis")
public class InformationAnalysisPo {

    /** 分析结果自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拥有该分析结果的用户账号主键。 */
    private Long userId;

    /** 被分析的信息主表主键。 */
    private Long informationId;

    /** 被分析的不可变信息快照主键。 */
    private Long snapshotId;

    /** 信息类型，当前真实实现仅支持 JOB。 */
    private String informationType;

    /** 平台定义的分析定义键，当前仅支持 JOB_USER_RELEVANCE。 */
    private String analysisDefinitionKey;

    /** 平台分析定义版本号，用于冻结 System Prompt 和 Schema 语义。 */
    private Integer analysisDefinitionVersion;

    /** 此次分析的业务目的，例如 USER_RELEVANCE。 */
    private String analysisPurpose;

    /** 分析使用的 Prompt 配置档案主键。 */
    private Long promptProfileId;

    /** 分析使用的不可变 Prompt 版本主键。 */
    private Long promptVersionId;

    /** 分析状态，例如 PENDING、RUNNING、SUCCEEDED 或 FAILED。 */
    private String status;

    /** Provider 返回并通过平台 Schema 校验后的 JSON 结果。 */
    private String resultJson;

    /** JOB 与用户需求的相关度分数，范围为 0 至 100。 */
    private Integer relevanceScore;

    /** 供列表和详情展示的分析摘要。 */
    private String summary;

    /** 调用前预估的输入 Token 数，不作为实际用量。 */
    private Long estimatedInputTokens;

    /** 调用前预估的输出 Token 数，不作为实际用量。 */
    private Long estimatedOutputTokens;

    /** 调用前预估的总 Token 数，不作为实际用量。 */
    private Long estimatedTotalTokens;

    /** Token 预估算法标识，用于解释预估值来源。 */
    private String estimateMethod;

    /** 分析失败时的平台稳定错误码。 */
    private String failureCode;

    /** 分析失败时供诊断使用的错误说明。 */
    private String failureMessage;

    /** 分析开始执行时间，按 UTC 保存。 */
    private LocalDateTime startedAt;

    /** 分析最终完成时间，按 UTC 保存。 */
    private LocalDateTime completedAt;

    /** 分析记录创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** 分析记录最近更新时间，按 UTC 保存。 */
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

    public String getAnalysisDefinitionKey() {
        return analysisDefinitionKey;
    }

    public void setAnalysisDefinitionKey(String analysisDefinitionKey) {
        this.analysisDefinitionKey = analysisDefinitionKey;
    }

    public Integer getAnalysisDefinitionVersion() {
        return analysisDefinitionVersion;
    }

    public void setAnalysisDefinitionVersion(Integer analysisDefinitionVersion) {
        this.analysisDefinitionVersion = analysisDefinitionVersion;
    }

    public String getAnalysisPurpose() {
        return analysisPurpose;
    }

    public void setAnalysisPurpose(String analysisPurpose) {
        this.analysisPurpose = analysisPurpose;
    }

    public Long getPromptProfileId() {
        return promptProfileId;
    }

    public void setPromptProfileId(Long promptProfileId) {
        this.promptProfileId = promptProfileId;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public void setPromptVersionId(Long promptVersionId) {
        this.promptVersionId = promptVersionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public Integer getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Integer relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Long getEstimatedInputTokens() {
        return estimatedInputTokens;
    }

    public void setEstimatedInputTokens(Long estimatedInputTokens) {
        this.estimatedInputTokens = estimatedInputTokens;
    }

    public Long getEstimatedOutputTokens() {
        return estimatedOutputTokens;
    }

    public void setEstimatedOutputTokens(Long estimatedOutputTokens) {
        this.estimatedOutputTokens = estimatedOutputTokens;
    }

    public Long getEstimatedTotalTokens() {
        return estimatedTotalTokens;
    }

    public void setEstimatedTotalTokens(Long estimatedTotalTokens) {
        this.estimatedTotalTokens = estimatedTotalTokens;
    }

    public String getEstimateMethod() {
        return estimateMethod;
    }

    public void setEstimateMethod(String estimateMethod) {
        this.estimateMethod = estimateMethod;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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
