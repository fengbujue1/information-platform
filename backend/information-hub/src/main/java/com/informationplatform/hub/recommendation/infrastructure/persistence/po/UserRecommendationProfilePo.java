package com.informationplatform.hub.recommendation.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射用户在一个 Information Type 下唯一的 Recommendation Profile Core。 */
@TableName("user_recommendation_profile")
public class UserRecommendationProfilePo {

    /** Recommendation Profile 自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Profile Owner 的用户账号主键。 */
    private Long userId;

    /** Profile 所属 Information Type；Phase 4 当前只实际使用 JOB。 */
    private String informationType;

    /** 绑定且属于同一 Owner 的 AI Prompt Profile 主键。 */
    private Long analysisPromptProfileId;

    /** 推荐候选回看窗口天数，范围 1 至 30。 */
    private Integer windowDays;

    /** 每轮推荐最大结果数，范围 1 至 100。 */
    private Integer topN;

    /** Core 与对应领域扩展组合后的规范化完整 Profile SHA-256。 */
    private String contentHash;

    /** Profile 创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** Profile 最近更新时间，按 UTC 保存。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getInformationType() { return informationType; }
    public void setInformationType(String informationType) { this.informationType = informationType; }
    public Long getAnalysisPromptProfileId() { return analysisPromptProfileId; }
    public void setAnalysisPromptProfileId(Long analysisPromptProfileId) {
        this.analysisPromptProfileId = analysisPromptProfileId;
    }
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays; }
    public Integer getTopN() { return topN; }
    public void setTopN(Integer topN) { this.topN = topN; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
