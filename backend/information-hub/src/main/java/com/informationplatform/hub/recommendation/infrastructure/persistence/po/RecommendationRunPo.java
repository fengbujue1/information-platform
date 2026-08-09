package com.informationplatform.hub.recommendation.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射一次冻结输入和算法身份的预计算 Recommendation Run。 */
@TableName("recommendation_run")
public class RecommendationRunPo {

    /** Recommendation Run 自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Run Owner 的用户账号主键。 */
    private Long userId;

    /** 触发类型：MANUAL 或 ANALYSIS_BATCH_COMPLETED。 */
    private String triggerType;

    /** 自动触发来源 Analysis Batch 主键；Manual Run 为空。 */
    private Long sourceAnalysisBatchId;

    /** Run 创建时使用的 Recommendation Profile 主键。 */
    private Long profileId;

    /** Run 冻结 Profile 的 SHA-256。 */
    private String profileContentHash;

    /** Run 创建时冻结的完整规范化 Profile JSON。 */
    private String profileSnapshotJson;

    /** Run 冻结的 AI Prompt Profile 主键。 */
    private Long promptProfileId;

    /** Run 冻结的不可变 AI Prompt Version 主键。 */
    private Long promptVersionId;

    /** 推荐算法稳定标识，V1 固定为 JOB_RECOMMENDATION。 */
    private String algorithmKey;

    /** 推荐算法版本，V1 为 1。 */
    private Integer algorithmVersion;

    /** 候选时间窗口起点，UTC 左闭区间。 */
    private LocalDateTime windowStart;

    /** 候选时间窗口终点，UTC 右开区间。 */
    private LocalDateTime windowEnd;

    /** 初始查询得到的候选数量。 */
    private Integer candidateCount;

    /** 完成资格与硬排除过滤后的候选数量。 */
    private Integer eligibleCount;

    /** 最终持久化的 Recommendation Item 数量。 */
    private Integer resultCount;

    /** Run 生命周期状态：PENDING、RUNNING、COMPLETED、FAILED 或 NOOP。 */
    private String status;

    /** NOOP 时的平台稳定跳过原因。 */
    private String skipReason;

    /** FAILED 时的平台稳定错误码。 */
    private String failureCode;

    /** FAILED 时供运维诊断的非敏感说明，最长 1000 字符。 */
    private String failureMessage;

    /** Run 开始执行时间，按 UTC 保存。 */
    private LocalDateTime startedAt;

    /** Run 到达终态时间，按 UTC 保存。 */
    private LocalDateTime completedAt;

    /** Run 创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** Run 最近更新时间，按 UTC 保存。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public Long getSourceAnalysisBatchId() { return sourceAnalysisBatchId; }
    public void setSourceAnalysisBatchId(Long sourceAnalysisBatchId) {
        this.sourceAnalysisBatchId = sourceAnalysisBatchId;
    }
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
    public String getProfileContentHash() { return profileContentHash; }
    public void setProfileContentHash(String profileContentHash) {
        this.profileContentHash = profileContentHash;
    }
    public String getProfileSnapshotJson() { return profileSnapshotJson; }
    public void setProfileSnapshotJson(String profileSnapshotJson) {
        this.profileSnapshotJson = profileSnapshotJson;
    }
    public Long getPromptProfileId() { return promptProfileId; }
    public void setPromptProfileId(Long promptProfileId) { this.promptProfileId = promptProfileId; }
    public Long getPromptVersionId() { return promptVersionId; }
    public void setPromptVersionId(Long promptVersionId) { this.promptVersionId = promptVersionId; }
    public String getAlgorithmKey() { return algorithmKey; }
    public void setAlgorithmKey(String algorithmKey) { this.algorithmKey = algorithmKey; }
    public Integer getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(Integer algorithmVersion) {
        this.algorithmVersion = algorithmVersion;
    }
    public LocalDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(LocalDateTime windowStart) { this.windowStart = windowStart; }
    public LocalDateTime getWindowEnd() { return windowEnd; }
    public void setWindowEnd(LocalDateTime windowEnd) { this.windowEnd = windowEnd; }
    public Integer getCandidateCount() { return candidateCount; }
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
    public Integer getEligibleCount() { return eligibleCount; }
    public void setEligibleCount(Integer eligibleCount) { this.eligibleCount = eligibleCount; }
    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
