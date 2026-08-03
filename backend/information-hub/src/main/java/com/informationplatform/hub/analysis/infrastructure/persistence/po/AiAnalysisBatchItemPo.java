package com.informationplatform.hub.analysis.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射分析批次中被冻结的候选信息。 */
@TableName("ai_analysis_batch_item")
public class AiAnalysisBatchItemPo {

    /** 分析批次明细自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属分析批次主键。 */
    private Long batchId;

    /** 被选中信息的主表主键。 */
    private Long informationId;

    /** 被冻结的不可变信息快照主键。 */
    private Long snapshotId;

    /** 对应的逻辑分析结果主键，尚未创建分析时允许为空。 */
    private Long analysisId;

    /** 候选在批次中的稳定选择顺序，从 1 开始。 */
    private Integer selectionOrder;

    /** 批次明细状态，例如 SELECTED、RUNNING、SUCCEEDED 或 FAILED。 */
    private String status;

    /** 候选被选择、跳过或延后的稳定原因。 */
    private String decisionReason;

    /** 该候选预估输入 Token 数，不作为实际用量。 */
    private Long estimatedInputTokens;

    /** 该候选预估输出 Token 数，不作为实际用量。 */
    private Long estimatedOutputTokens;

    /** 该候选预估 Token 总数，不作为实际用量。 */
    private Long estimatedTotalTokens;

    /** 批次明细开始执行时间，按 UTC 保存。 */
    private LocalDateTime startedAt;

    /** 批次明细最终完成时间，按 UTC 保存。 */
    private LocalDateTime completedAt;

    /** 批次明细创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** 批次明细最近更新时间，按 UTC 保存。 */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
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

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public Integer getSelectionOrder() {
        return selectionOrder;
    }

    public void setSelectionOrder(Integer selectionOrder) {
        this.selectionOrder = selectionOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
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
