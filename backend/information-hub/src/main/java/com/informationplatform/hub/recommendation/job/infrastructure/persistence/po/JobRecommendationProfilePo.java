package com.informationplatform.hub.recommendation.job.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射 Recommendation Profile Core 的 JOB 专属 1:1 偏好扩展。 */
@TableName("job_recommendation_profile")
public class JobRecommendationProfilePo {

    /** Profile Core 主键，同时作为本扩展表的输入型主键。 */
    @TableId(type = IdType.INPUT)
    private Long profileId;

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

    /** JOB Candidate hard exclusion 关键词字符串数组 JSON。 */
    private String excludedKeywords;

    /** JOB Profile 扩展创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** JOB Profile 扩展最近更新时间，按 UTC 保存。 */
    private LocalDateTime updatedAt;

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
