package com.informationplatform.hub.recommendation.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射用户唯一的当前 Recommendation Profile。 */
@TableName("user_recommendation_profile")
public class UserRecommendationProfilePo {

    /** Recommendation Profile 自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Profile Owner 的用户账号主键。 */
    private Long userId;

    /** 绑定且属于同一 Owner 的 AI Prompt Profile 主键。 */
    private Long analysisPromptProfileId;

    /** 推荐候选回看窗口天数，范围 1 至 30。 */
    private Integer windowDays;

    /** 每轮推荐最大结果数，范围 1 至 100。 */
    private Integer topN;

    /** 规范化目标岗位字符串数组 JSON。 */
    private String targetRoles;

    /** 规范化偏好技能字符串数组 JSON。 */
    private String preferredSkills;

    /** 规范化偏好城市字符串数组 JSON。 */
    private String preferredCities;

    /** 规范化远程类型字符串数组 JSON。 */
    private String preferredRemoteTypes;

    /** 最低期望月薪，单位为人民币元。 */
    private Integer salaryMinMonthlyYuan;

    /** 作为候选硬排除条件的关键词字符串数组 JSON。 */
    private String excludedKeywords;

    /** 规范化完整 Profile 的 SHA-256，固定 64 个十六进制字符。 */
    private String contentHash;

    /** Profile 创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** Profile 最近更新时间，按 UTC 保存。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAnalysisPromptProfileId() { return analysisPromptProfileId; }
    public void setAnalysisPromptProfileId(Long analysisPromptProfileId) {
        this.analysisPromptProfileId = analysisPromptProfileId;
    }
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays; }
    public Integer getTopN() { return topN; }
    public void setTopN(Integer topN) { this.topN = topN; }
    public String getTargetRoles() { return targetRoles; }
    public void setTargetRoles(String targetRoles) { this.targetRoles = targetRoles; }
    public String getPreferredSkills() { return preferredSkills; }
    public void setPreferredSkills(String preferredSkills) { this.preferredSkills = preferredSkills; }
    public String getPreferredCities() { return preferredCities; }
    public void setPreferredCities(String preferredCities) { this.preferredCities = preferredCities; }
    public String getPreferredRemoteTypes() { return preferredRemoteTypes; }
    public void setPreferredRemoteTypes(String preferredRemoteTypes) {
        this.preferredRemoteTypes = preferredRemoteTypes;
    }
    public Integer getSalaryMinMonthlyYuan() { return salaryMinMonthlyYuan; }
    public void setSalaryMinMonthlyYuan(Integer salaryMinMonthlyYuan) {
        this.salaryMinMonthlyYuan = salaryMinMonthlyYuan;
    }
    public String getExcludedKeywords() { return excludedKeywords; }
    public void setExcludedKeywords(String excludedKeywords) { this.excludedKeywords = excludedKeywords; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
