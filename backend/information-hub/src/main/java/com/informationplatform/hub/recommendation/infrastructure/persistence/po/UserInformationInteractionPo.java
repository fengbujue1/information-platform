package com.informationplatform.hub.recommendation.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射用户与 Information 之间跨快照保留的当前交互聚合状态。 */
@TableName("user_information_interaction")
public class UserInformationInteractionPo {

    /** 用户信息交互记录自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 交互 Owner 的用户账号主键。 */
    private Long userId;

    /** 被交互 Information 主键，状态不随 Snapshot 变化而重置。 */
    private Long informationId;

    /** 用户累计查看次数，最小值为 0。 */
    private Integer viewCount;

    /** 最近一次查看时间，按 UTC 保存。 */
    private LocalDateTime lastViewedAt;

    /** 推荐反馈状态：NONE、INTERESTED 或 NOT_INTERESTED。 */
    private String feedbackState;

    /** 最近一次反馈修改时间，按 UTC 保存。 */
    private LocalDateTime feedbackUpdatedAt;

    /** 求职处理状态：NONE、CONTACTED 或 CONTACTED_NOT_SUITABLE。 */
    private String jobDisposition;

    /** 最近一次求职处理状态修改时间，按 UTC 保存。 */
    private LocalDateTime dispositionUpdatedAt;

    /** 最近一次产生交互归因的 Recommendation Item 主键，可为空。 */
    private Long lastRecommendationItemId;

    /** 交互聚合记录创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** 交互聚合记录最近更新时间，按 UTC 保存。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getInformationId() { return informationId; }
    public void setInformationId(Long informationId) { this.informationId = informationId; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public LocalDateTime getLastViewedAt() { return lastViewedAt; }
    public void setLastViewedAt(LocalDateTime lastViewedAt) { this.lastViewedAt = lastViewedAt; }
    public String getFeedbackState() { return feedbackState; }
    public void setFeedbackState(String feedbackState) { this.feedbackState = feedbackState; }
    public LocalDateTime getFeedbackUpdatedAt() { return feedbackUpdatedAt; }
    public void setFeedbackUpdatedAt(LocalDateTime feedbackUpdatedAt) {
        this.feedbackUpdatedAt = feedbackUpdatedAt;
    }
    public String getJobDisposition() { return jobDisposition; }
    public void setJobDisposition(String jobDisposition) { this.jobDisposition = jobDisposition; }
    public LocalDateTime getDispositionUpdatedAt() { return dispositionUpdatedAt; }
    public void setDispositionUpdatedAt(LocalDateTime dispositionUpdatedAt) {
        this.dispositionUpdatedAt = dispositionUpdatedAt;
    }
    public Long getLastRecommendationItemId() { return lastRecommendationItemId; }
    public void setLastRecommendationItemId(Long lastRecommendationItemId) {
        this.lastRecommendationItemId = lastRecommendationItemId;
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
