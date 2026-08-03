package com.informationplatform.hub.analysis.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射一次真实模型请求及 Provider 返回的实际用量。 */
@TableName("ai_model_invocation")
public class AiModelInvocationPo {

    /** 模型调用自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属逻辑分析结果主键。 */
    private Long analysisId;

    /** 拥有该模型调用记录的用户账号主键。 */
    private Long userId;

    /** 触发该调用的批次明细主键，非批次调用时允许为空。 */
    private Long batchItemId;

    /** OpenAI-compatible Provider 的平台配置标识。 */
    private String provider;

    /** Provider 接收请求时使用的模型名称。 */
    private String modelName;

    /** Provider 返回的请求标识，未返回时允许为空。 */
    private String providerRequestId;

    /** 同一逻辑分析内的模型调用尝试序号，从 1 开始。 */
    private Integer attemptNo;

    /** 模型调用状态，例如 RUNNING、SUCCEEDED 或 FAILED。 */
    private String status;

    /** Provider 返回的结束原因，未返回时允许为空。 */
    private String finishReason;

    /** Provider Usage 返回的实际输入 Token 数，缺失时必须为空。 */
    private Long inputTokens;

    /** Provider Usage 返回的实际输出 Token 数，缺失时必须为空。 */
    private Long outputTokens;

    /** Provider Usage 返回的实际总 Token 数，缺失时必须为空。 */
    private Long totalTokens;

    /** Provider Usage 返回的缓存输入 Token 数，未提供时必须为空。 */
    private Long cachedInputTokens;

    /** Provider Usage 返回的推理 Token 数，未提供时必须为空。 */
    private Long reasoningTokens;

    /** 实际用量状态，仅允许 REPORTED 或 UNAVAILABLE。 */
    private String usageStatus;

    /** 从发起请求到收到响应的耗时，单位毫秒。 */
    private Long latencyMs;

    /** 调用失败时的平台稳定错误码。 */
    private String errorCode;

    /** 调用失败时供诊断使用的错误说明。 */
    private String errorMessage;

    /** 模型调用开始时间，按 UTC 保存。 */
    private LocalDateTime startedAt;

    /** 模型调用完成时间，按 UTC 保存。 */
    private LocalDateTime completedAt;

    /** 模型调用记录创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getBatchItemId() {
        return batchItemId;
    }

    public void setBatchItemId(Long batchItemId) {
        this.batchItemId = batchItemId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getProviderRequestId() {
        return providerRequestId;
    }

    public void setProviderRequestId(String providerRequestId) {
        this.providerRequestId = providerRequestId;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public Long getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Long inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Long getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Long outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Long getCachedInputTokens() {
        return cachedInputTokens;
    }

    public void setCachedInputTokens(Long cachedInputTokens) {
        this.cachedInputTokens = cachedInputTokens;
    }

    public Long getReasoningTokens() {
        return reasoningTokens;
    }

    public void setReasoningTokens(Long reasoningTokens) {
        this.reasoningTokens = reasoningTokens;
    }

    public String getUsageStatus() {
        return usageStatus;
    }

    public void setUsageStatus(String usageStatus) {
        this.usageStatus = usageStatus;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
}
