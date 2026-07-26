package com.informationplatform.hub.ingestion.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public record JobExtensionRequest(
        /** 来源系统中的公司标识。 */
        @Size(max = 255) String sourceCompanyId,
        /** 来源系统中的招聘者标识。 */
        @Size(max = 255) String sourceRecruiterId,
        /** 公司名称。 */
        @Size(max = 255) String companyName,
        /** 公司详情页地址。 */
        @Size(max = 2048) String companyUrl,
        /** 公司规模原始文本。 */
        @Size(max = 100) String companyScaleText,
        /** 公司融资阶段原始文本。 */
        @Size(max = 100) String companyStageText,
        /** 公司所属行业原始文本。 */
        @Size(max = 255) String companyIndustryText,
        /** 职位薪资原始文本。 */
        @Size(max = 100) String salaryText,
        /** 薪资信息的来源或推导方式。 */
        @Size(max = 32) String salarySource,
        /** 估算的最低月薪，单位为人民币元。 */
        @PositiveOrZero Integer salaryMinMonthlyYuan,
        /** 估算的最高月薪，单位为人民币元。 */
        @PositiveOrZero Integer salaryMaxMonthlyYuan,
        /** 每年发薪月数。 */
        @PositiveOrZero @Max(255) Integer salaryMonths,
        /** 来源系统展示的完整工作地点。 */
        @Size(max = 255) String locationName,
        /** 工作城市。 */
        @Size(max = 100) String cityName,
        /** 工作行政区。 */
        @Size(max = 100) String areaName,
        /** 工作商圈。 */
        @Size(max = 100) String businessDistrictName,
        /** 工作经验要求原始文本。 */
        @Size(max = 100) String experienceText,
        /** 学历要求原始文本。 */
        @Size(max = 100) String educationText,
        /** 招聘者姓名。 */
        @Size(max = 255) String recruiterName,
        /** 招聘者职位。 */
        @Size(max = 255) String recruiterTitle,
        /** 招聘者活跃状态原始文本。 */
        @Size(max = 100) String recruiterActiveText,
        /** 办公方式：未知、现场、混合或远程。 */
        @Pattern(regexp = "UNKNOWN|ONSITE|HYBRID|REMOTE") String remoteType,
        /** 来源职位状态：未知、招聘中或已下线。 */
        @Pattern(regexp = "UNKNOWN|ACTIVE|OFFLINE") String jobStatus,
        /** 职位详情采集状态。 */
        @Pattern(regexp = "UNKNOWN|FETCHED|FAILED|UNAVAILABLE") String detailStatus,
        /** 职位详情成功采集或尝试采集的时间。 */
        OffsetDateTime detailCollectedAt,
        /** 来源系统返回的职位标签。 */
        List<@Size(max = 255) String> sourceTags,
        /** 来源系统返回的技能标签。 */
        List<@Size(max = 255) String> sourceSkillTags,
        /** 来源系统返回的福利标签。 */
        List<@Size(max = 255) String> welfare) {}
