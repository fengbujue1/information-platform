package com.informationplatform.hub.analysis.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射手动与定时分析共用的冻结批次。 */
@TableName("ai_analysis_batch")
public class AiAnalysisBatchPo {

    /** 分析批次自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拥有该分析批次的用户账号主键。 */
    private Long userId;

    /** 批次触发类型，仅允许 MANUAL 或 SCHEDULED。 */
    private String triggerType;

    /** 手动请求幂等 UUID，仅手动批次有值。 */
    private String manualRequestId;

    /** 触发该批次的分析计划主键，仅定时批次有值。 */
    private Long scheduleId;

    /** 本次计划触发对应的 UTC 调度时刻，仅定时批次有值。 */
    private LocalDateTime scheduledFor;

    /** 批次处理的信息类型，当前真实实现仅支持 JOB。 */
    private String informationType;

    /** 平台定义的分析定义键，当前仅支持 JOB_USER_RELEVANCE。 */
    private String analysisDefinitionKey;

    /** 批次冻结的平台分析定义版本号。 */
    private Integer analysisDefinitionVersion;

    /** 批次冻结的 Prompt 配置档案主键。 */
    private Long promptProfileId;

    /** 批次冻结的不可变 Prompt 版本主键。 */
    private Long promptVersionId;

    /** 候选时间窗口的口径，当前固定为 FIRST_INGESTED。 */
    private String windowBasis;

    /** 用户请求的候选回看窗口天数。 */
    private Integer requestedWindowDays;

    /** 冻结候选窗口起点，按 UTC 保存并采用左闭区间。 */
    private LocalDateTime windowStart;

    /** 冻结候选窗口终点，按 UTC 保存并采用右开区间。 */
    private LocalDateTime windowEnd;

    /** 用户请求的最大候选数量。 */
    private Integer requestedMaxCandidates;

    /** 用户请求的预估 Token 预算上限。 */
    private Long requestedTokenBudget;

    /** 时间窗口内查询到的信息总数。 */
    private Integer totalInWindow;

    /** 排除无效候选后符合分析条件的信息数。 */
    private Integer eligibleCount;

    /** 已存在可复用成功分析结果的信息数。 */
    private Integer alreadyAnalyzedCount;

    /** 最终冻结进批次明细的信息数。 */
    private Integer selectedCount;

    /** 因候选数量上限而延后的信息数。 */
    private Integer deferredByItemLimitCount;

    /** 因预估 Token 预算上限而延后的信息数。 */
    private Integer deferredByTokenBudgetCount;

    /** 批次预估输入 Token 总数，不作为实际用量。 */
    private Long estimatedInputTokens;

    /** 批次预估输出 Token 总数，不作为实际用量。 */
    private Long estimatedOutputTokens;

    /** 批次预估 Token 总数，不作为实际用量。 */
    private Long estimatedTotalTokens;

    /** Token 预估算法标识，用于解释预估值来源。 */
    private String estimateMethod;

    /** 批次状态，例如 PENDING、RUNNING、SUCCEEDED 或 FAILED。 */
    private String status;

    /** 批次未执行时的平台稳定跳过原因。 */
    private String skipReason;

    /** 批次开始执行时间，按 UTC 保存。 */
    private LocalDateTime startedAt;

    /** 批次最终完成时间，按 UTC 保存。 */
    private LocalDateTime completedAt;

    /** 批次记录创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** 批次记录最近更新时间，按 UTC 保存。 */
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

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getManualRequestId() {
        return manualRequestId;
    }

    public void setManualRequestId(String manualRequestId) {
        this.manualRequestId = manualRequestId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(LocalDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
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

    public String getWindowBasis() {
        return windowBasis;
    }

    public void setWindowBasis(String windowBasis) {
        this.windowBasis = windowBasis;
    }

    public Integer getRequestedWindowDays() {
        return requestedWindowDays;
    }

    public void setRequestedWindowDays(Integer requestedWindowDays) {
        this.requestedWindowDays = requestedWindowDays;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalDateTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public Integer getRequestedMaxCandidates() {
        return requestedMaxCandidates;
    }

    public void setRequestedMaxCandidates(Integer requestedMaxCandidates) {
        this.requestedMaxCandidates = requestedMaxCandidates;
    }

    public Long getRequestedTokenBudget() {
        return requestedTokenBudget;
    }

    public void setRequestedTokenBudget(Long requestedTokenBudget) {
        this.requestedTokenBudget = requestedTokenBudget;
    }

    public Integer getTotalInWindow() {
        return totalInWindow;
    }

    public void setTotalInWindow(Integer totalInWindow) {
        this.totalInWindow = totalInWindow;
    }

    public Integer getEligibleCount() {
        return eligibleCount;
    }

    public void setEligibleCount(Integer eligibleCount) {
        this.eligibleCount = eligibleCount;
    }

    public Integer getAlreadyAnalyzedCount() {
        return alreadyAnalyzedCount;
    }

    public void setAlreadyAnalyzedCount(Integer alreadyAnalyzedCount) {
        this.alreadyAnalyzedCount = alreadyAnalyzedCount;
    }

    public Integer getSelectedCount() {
        return selectedCount;
    }

    public void setSelectedCount(Integer selectedCount) {
        this.selectedCount = selectedCount;
    }

    public Integer getDeferredByItemLimitCount() {
        return deferredByItemLimitCount;
    }

    public void setDeferredByItemLimitCount(Integer deferredByItemLimitCount) {
        this.deferredByItemLimitCount = deferredByItemLimitCount;
    }

    public Integer getDeferredByTokenBudgetCount() {
        return deferredByTokenBudgetCount;
    }

    public void setDeferredByTokenBudgetCount(Integer deferredByTokenBudgetCount) {
        this.deferredByTokenBudgetCount = deferredByTokenBudgetCount;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public void setSkipReason(String skipReason) {
        this.skipReason = skipReason;
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
