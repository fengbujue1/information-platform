package com.informationplatform.hub.job.infrastructure.persistence.query;

import java.time.LocalDateTime;

/**
 * information_item 与 job_information 关联查询结果。
 *
 * <p>该对象只承载标准化字段，不声明 rawPayload，从持久层避免查询接口误返回原始数据。
 */
public class JobQueryRow {

    /** 平台信息主键。 */
    private Long id;

    /** 信息来源。 */
    private String source;

    /** 来源系统中的职位唯一标识。 */
    private String sourceItemId;

    /** 来源职位详情页地址。 */
    private String sourceUrl;

    /** 职位标题。 */
    private String title;

    /** 职位正文。 */
    private String content;

    /** 来源发布时间。 */
    private LocalDateTime publishTime;

    /** 最近一次数据采集时间。 */
    private LocalDateTime collectedAt;

    /** 平台首次发现时间。 */
    private LocalDateTime firstSeenTime;

    /** 平台最近接收时间。 */
    private LocalDateTime lastSeenTime;

    /** 当前标准化业务版本号。 */
    private Integer currentVersionNo;

    /** 最近一次上报的采集器标识。 */
    private String collectorId;

    /** 最近一次上报的采集器版本。 */
    private String collectorVersion;

    /** 来源系统中的公司标识。 */
    private String sourceCompanyId;

    /** 来源系统中的招聘者标识。 */
    private String sourceRecruiterId;

    /** 公司名称。 */
    private String companyName;

    /** 公司详情页地址。 */
    private String companyUrl;

    /** 公司规模原始文本。 */
    private String companyScaleText;

    /** 公司融资阶段原始文本。 */
    private String companyStageText;

    /** 公司所属行业原始文本。 */
    private String companyIndustryText;

    /** 来源展示的薪资文本。 */
    private String salaryText;

    /** 薪资信息来源或推导方式。 */
    private String salarySource;

    /** 估算最低月薪，单位为人民币元。 */
    private Integer salaryMinMonthlyYuan;

    /** 估算最高月薪，单位为人民币元。 */
    private Integer salaryMaxMonthlyYuan;

    /** 每年发薪月数。 */
    private Integer salaryMonths;

    /** 来源展示的完整工作地点。 */
    private String locationName;

    /** 工作城市。 */
    private String cityName;

    /** 工作行政区。 */
    private String areaName;

    /** 工作商圈。 */
    private String businessDistrictName;

    /** 工作经验要求。 */
    private String experienceText;

    /** 学历要求。 */
    private String educationText;

    /** 招聘者姓名。 */
    private String recruiterName;

    /** 招聘者职位。 */
    private String recruiterTitle;

    /** 最近一次被 Collector 观察到在线的来源文本。 */
    private String recruiterActiveText;

    /** 办公方式。 */
    private String remoteType;

    /** 来源职位状态。 */
    private String jobStatus;

    /** 职位详情采集状态。 */
    private String detailStatus;

    /** 职位详情采集或尝试时间。 */
    private LocalDateTime detailCollectedAt;

    /** 来源职位标签 JSON 数组文本。 */
    private String sourceTags;

    /** 来源技能标签 JSON 数组文本。 */
    private String sourceSkillTags;

    /** 来源福利标签 JSON 数组文本。 */
    private String welfare;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceItemId() {
        return sourceItemId;
    }

    public void setSourceItemId(String sourceItemId) {
        this.sourceItemId = sourceItemId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public LocalDateTime getFirstSeenTime() {
        return firstSeenTime;
    }

    public void setFirstSeenTime(LocalDateTime firstSeenTime) {
        this.firstSeenTime = firstSeenTime;
    }

    public LocalDateTime getLastSeenTime() {
        return lastSeenTime;
    }

    public void setLastSeenTime(LocalDateTime lastSeenTime) {
        this.lastSeenTime = lastSeenTime;
    }

    public Integer getCurrentVersionNo() {
        return currentVersionNo;
    }

    public void setCurrentVersionNo(Integer currentVersionNo) {
        this.currentVersionNo = currentVersionNo;
    }

    public String getCollectorId() {
        return collectorId;
    }

    public void setCollectorId(String collectorId) {
        this.collectorId = collectorId;
    }

    public String getCollectorVersion() {
        return collectorVersion;
    }

    public void setCollectorVersion(String collectorVersion) {
        this.collectorVersion = collectorVersion;
    }

    public String getSourceCompanyId() {
        return sourceCompanyId;
    }

    public void setSourceCompanyId(String sourceCompanyId) {
        this.sourceCompanyId = sourceCompanyId;
    }

    public String getSourceRecruiterId() {
        return sourceRecruiterId;
    }

    public void setSourceRecruiterId(String sourceRecruiterId) {
        this.sourceRecruiterId = sourceRecruiterId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyUrl() {
        return companyUrl;
    }

    public void setCompanyUrl(String companyUrl) {
        this.companyUrl = companyUrl;
    }

    public String getCompanyScaleText() {
        return companyScaleText;
    }

    public void setCompanyScaleText(String companyScaleText) {
        this.companyScaleText = companyScaleText;
    }

    public String getCompanyStageText() {
        return companyStageText;
    }

    public void setCompanyStageText(String companyStageText) {
        this.companyStageText = companyStageText;
    }

    public String getCompanyIndustryText() {
        return companyIndustryText;
    }

    public void setCompanyIndustryText(String companyIndustryText) {
        this.companyIndustryText = companyIndustryText;
    }

    public String getSalaryText() {
        return salaryText;
    }

    public void setSalaryText(String salaryText) {
        this.salaryText = salaryText;
    }

    public String getSalarySource() {
        return salarySource;
    }

    public void setSalarySource(String salarySource) {
        this.salarySource = salarySource;
    }

    public Integer getSalaryMinMonthlyYuan() {
        return salaryMinMonthlyYuan;
    }

    public void setSalaryMinMonthlyYuan(Integer salaryMinMonthlyYuan) {
        this.salaryMinMonthlyYuan = salaryMinMonthlyYuan;
    }

    public Integer getSalaryMaxMonthlyYuan() {
        return salaryMaxMonthlyYuan;
    }

    public void setSalaryMaxMonthlyYuan(Integer salaryMaxMonthlyYuan) {
        this.salaryMaxMonthlyYuan = salaryMaxMonthlyYuan;
    }

    public Integer getSalaryMonths() {
        return salaryMonths;
    }

    public void setSalaryMonths(Integer salaryMonths) {
        this.salaryMonths = salaryMonths;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getBusinessDistrictName() {
        return businessDistrictName;
    }

    public void setBusinessDistrictName(String businessDistrictName) {
        this.businessDistrictName = businessDistrictName;
    }

    public String getExperienceText() {
        return experienceText;
    }

    public void setExperienceText(String experienceText) {
        this.experienceText = experienceText;
    }

    public String getEducationText() {
        return educationText;
    }

    public void setEducationText(String educationText) {
        this.educationText = educationText;
    }

    public String getRecruiterName() {
        return recruiterName;
    }

    public void setRecruiterName(String recruiterName) {
        this.recruiterName = recruiterName;
    }

    public String getRecruiterTitle() {
        return recruiterTitle;
    }

    public void setRecruiterTitle(String recruiterTitle) {
        this.recruiterTitle = recruiterTitle;
    }

    public String getRecruiterActiveText() {
        return recruiterActiveText;
    }

    public void setRecruiterActiveText(String recruiterActiveText) {
        this.recruiterActiveText = recruiterActiveText;
    }

    public String getRemoteType() {
        return remoteType;
    }

    public void setRemoteType(String remoteType) {
        this.remoteType = remoteType;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getDetailStatus() {
        return detailStatus;
    }

    public void setDetailStatus(String detailStatus) {
        this.detailStatus = detailStatus;
    }

    public LocalDateTime getDetailCollectedAt() {
        return detailCollectedAt;
    }

    public void setDetailCollectedAt(LocalDateTime detailCollectedAt) {
        this.detailCollectedAt = detailCollectedAt;
    }

    public String getSourceTags() {
        return sourceTags;
    }

    public void setSourceTags(String sourceTags) {
        this.sourceTags = sourceTags;
    }

    public String getSourceSkillTags() {
        return sourceSkillTags;
    }

    public void setSourceSkillTags(String sourceSkillTags) {
        this.sourceSkillTags = sourceSkillTags;
    }

    public String getWelfare() {
        return welfare;
    }

    public void setWelfare(String welfare) {
        this.welfare = welfare;
    }
}
