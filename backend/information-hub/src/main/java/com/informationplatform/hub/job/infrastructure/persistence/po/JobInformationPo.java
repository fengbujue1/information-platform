package com.informationplatform.hub.job.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("job_information")
public class JobInformationPo {

    /** 对应 information_item 的主键，同时也是本表主键。 */
    @TableId(type = IdType.INPUT)
    private Long informationId;

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

    /** 职位薪资原始文本。 */
    private String salaryText;

    /** 薪资信息的来源或推导方式。 */
    private String salarySource;

    /** 估算的最低月薪，单位为人民币元。 */
    private Integer salaryMinMonthlyYuan;

    /** 估算的最高月薪，单位为人民币元。 */
    private Integer salaryMaxMonthlyYuan;

    /** 每年发薪月数。 */
    private Integer salaryMonths;

    /** 来源系统展示的完整工作地点。 */
    private String locationName;

    /** 工作城市。 */
    private String cityName;

    /** 工作行政区。 */
    private String areaName;

    /** 工作商圈。 */
    private String businessDistrictName;

    /** 工作经验要求原始文本。 */
    private String experienceText;

    /** 学历要求原始文本。 */
    private String educationText;

    /** 招聘者姓名。 */
    private String recruiterName;

    /** 招聘者职位。 */
    private String recruiterTitle;

    /** 招聘者活跃状态原始文本。 */
    private String recruiterActiveText;

    /** 办公方式：未知、现场、混合或远程。 */
    private String remoteType;

    /** 来源职位状态：未知、招聘中或已下线。 */
    private String jobStatus;

    /** 职位详情采集状态。 */
    private String detailStatus;

    /** 职位详情成功采集或尝试采集的时间。 */
    private LocalDateTime detailCollectedAt;

    /** 来源系统返回的职位标签 JSON 数组。 */
    private String sourceTags;

    /** 来源系统返回的技能标签 JSON 数组。 */
    private String sourceSkillTags;

    /** 来源系统返回的福利标签 JSON 数组。 */
    private String welfare;

    public Long getInformationId() {
        return informationId;
    }

    public void setInformationId(Long informationId) {
        this.informationId = informationId;
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
