package com.informationplatform.hub.recommendation.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 映射 Recommendation Run 产生的不可变排序结果事实。 */
@TableName("recommendation_item")
public class RecommendationItemPo {

    /** Recommendation Item 自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 Recommendation Run 主键。 */
    private Long runId;

    /** 被推荐 Information 主键。 */
    private Long informationId;

    /** 推荐计算使用的不可变 Information Snapshot 主键。 */
    private Long snapshotId;

    /** 推荐计算消费的成功 Information Analysis 主键。 */
    private Long analysisId;

    /** Run 内从 1 开始的稳定排名。 */
    private Integer rankNo;

    /** 最终推荐分数，范围 0 至 100，保留三位小数。 */
    private BigDecimal finalScore;

    /** AI 相关度分数，范围 0 至 100，保留三位小数。 */
    private BigDecimal aiRelevanceScore;

    /** 结构化 Profile 匹配分数，范围 0 至 100，保留三位小数。 */
    private BigDecimal profileMatchScore;

    /** 基于首次入库时间计算的新鲜度分数，范围 0 至 100，保留三位小数。 */
    private BigDecimal freshnessScore;

    /** 可解释评分分项 JSON。 */
    private String scoreBreakdownJson;

    /** 面向用户展示的推荐原因字符串数组 JSON。 */
    private String reasonsJson;

    /** 公司、标题与城市规范化后计算的确定性 SHA-256 去重键。 */
    private String duplicateGroupKey;

    /** Recommendation Item 创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public Long getInformationId() { return informationId; }
    public void setInformationId(Long informationId) { this.informationId = informationId; }
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public Long getAnalysisId() { return analysisId; }
    public void setAnalysisId(Long analysisId) { this.analysisId = analysisId; }
    public Integer getRankNo() { return rankNo; }
    public void setRankNo(Integer rankNo) { this.rankNo = rankNo; }
    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
    public BigDecimal getAiRelevanceScore() { return aiRelevanceScore; }
    public void setAiRelevanceScore(BigDecimal aiRelevanceScore) {
        this.aiRelevanceScore = aiRelevanceScore;
    }
    public BigDecimal getProfileMatchScore() { return profileMatchScore; }
    public void setProfileMatchScore(BigDecimal profileMatchScore) {
        this.profileMatchScore = profileMatchScore;
    }
    public BigDecimal getFreshnessScore() { return freshnessScore; }
    public void setFreshnessScore(BigDecimal freshnessScore) { this.freshnessScore = freshnessScore; }
    public String getScoreBreakdownJson() { return scoreBreakdownJson; }
    public void setScoreBreakdownJson(String scoreBreakdownJson) {
        this.scoreBreakdownJson = scoreBreakdownJson;
    }
    public String getReasonsJson() { return reasonsJson; }
    public void setReasonsJson(String reasonsJson) { this.reasonsJson = reasonsJson; }
    public String getDuplicateGroupKey() { return duplicateGroupKey; }
    public void setDuplicateGroupKey(String duplicateGroupKey) {
        this.duplicateGroupKey = duplicateGroupKey;
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
