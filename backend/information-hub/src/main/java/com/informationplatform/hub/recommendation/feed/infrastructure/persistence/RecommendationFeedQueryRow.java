package com.informationplatform.hub.recommendation.feed.infrastructure.persistence;

import java.math.BigDecimal;

/** Recommendation Item、当前 Interaction 与 JOB 展示字段的只读查询投影。 */
public class RecommendationFeedQueryRow {

    /** Recommendation Item 主键。 */
    private Long recommendationItemId;
    /** Information 主键。 */
    private Long informationId;
    /** 冻结 Snapshot 主键。 */
    private Long snapshotId;
    /** Run 内稳定排名。 */
    private Integer rankNo;
    /** 最终推荐分数。 */
    private BigDecimal finalScore;
    /** AI 相关度分数。 */
    private BigDecimal aiRelevanceScore;
    /** Profile 匹配分数。 */
    private BigDecimal profileMatchScore;
    /** 新鲜度分数。 */
    private BigDecimal freshnessScore;
    /** 推荐原因 JSON 字符串数组。 */
    private String reasonsJson;
    /** 当前通用 Feedback 状态。 */
    private String feedbackState;
    /** 当前 JOB disposition 状态。 */
    private String jobDisposition;
    /** 当前用户累计查看次数。 */
    private Integer viewCount;
    /** 当前职位标题。 */
    private String title;
    /** 当前公司名称。 */
    private String companyName;
    /** 当前薪资展示文本。 */
    private String salaryText;
    /** 当前完整工作地点。 */
    private String locationName;
    /** 当前办公方式。 */
    private String remoteType;
    /** 当前来源详情页地址。 */
    private String sourceUrl;

    public Long getRecommendationItemId() { return recommendationItemId; }
    public void setRecommendationItemId(Long recommendationItemId) { this.recommendationItemId = recommendationItemId; }
    public Long getInformationId() { return informationId; }
    public void setInformationId(Long informationId) { this.informationId = informationId; }
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public Integer getRankNo() { return rankNo; }
    public void setRankNo(Integer rankNo) { this.rankNo = rankNo; }
    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
    public BigDecimal getAiRelevanceScore() { return aiRelevanceScore; }
    public void setAiRelevanceScore(BigDecimal aiRelevanceScore) { this.aiRelevanceScore = aiRelevanceScore; }
    public BigDecimal getProfileMatchScore() { return profileMatchScore; }
    public void setProfileMatchScore(BigDecimal profileMatchScore) { this.profileMatchScore = profileMatchScore; }
    public BigDecimal getFreshnessScore() { return freshnessScore; }
    public void setFreshnessScore(BigDecimal freshnessScore) { this.freshnessScore = freshnessScore; }
    public String getReasonsJson() { return reasonsJson; }
    public void setReasonsJson(String reasonsJson) { this.reasonsJson = reasonsJson; }
    public String getFeedbackState() { return feedbackState; }
    public void setFeedbackState(String feedbackState) { this.feedbackState = feedbackState; }
    public String getJobDisposition() { return jobDisposition; }
    public void setJobDisposition(String jobDisposition) { this.jobDisposition = jobDisposition; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getSalaryText() { return salaryText; }
    public void setSalaryText(String salaryText) { this.salaryText = salaryText; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getRemoteType() { return remoteType; }
    public void setRemoteType(String remoteType) { this.remoteType = remoteType; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
}
