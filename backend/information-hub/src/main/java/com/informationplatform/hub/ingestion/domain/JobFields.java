package com.informationplatform.hub.ingestion.domain;

import java.time.Instant;
import java.util.List;

public record JobFields(
        /** 来源系统中的公司标识。 */
        String sourceCompanyId,
        /** 来源系统中的招聘者标识。 */
        String sourceRecruiterId,
        /** 公司名称。 */
        String companyName,
        /** 公司详情页地址。 */
        String companyUrl,
        /** 公司规模原始文本。 */
        String companyScaleText,
        /** 公司融资阶段原始文本。 */
        String companyStageText,
        /** 公司所属行业原始文本。 */
        String companyIndustryText,
        /** 职位薪资原始文本。 */
        String salaryText,
        /** 薪资信息的来源或推导方式。 */
        String salarySource,
        /** 估算的最低月薪，单位为人民币元。 */
        Integer salaryMinMonthlyYuan,
        /** 估算的最高月薪，单位为人民币元。 */
        Integer salaryMaxMonthlyYuan,
        /** 每年发薪月数。 */
        Integer salaryMonths,
        /** 来源系统展示的完整工作地点。 */
        String locationName,
        /** 工作城市。 */
        String cityName,
        /** 工作行政区。 */
        String areaName,
        /** 工作商圈。 */
        String businessDistrictName,
        /** 工作经验要求原始文本。 */
        String experienceText,
        /** 学历要求原始文本。 */
        String educationText,
        /** 招聘者姓名。 */
        String recruiterName,
        /** 招聘者职位。 */
        String recruiterTitle,
        /** 招聘者活跃状态原始文本。 */
        String recruiterActiveText,
        /** 办公方式：未知、现场、混合或远程。 */
        String remoteType,
        /** 来源职位状态：未知、招聘中或已下线。 */
        String jobStatus,
        /** 职位详情采集状态。 */
        String detailStatus,
        /** 职位详情成功采集或尝试采集的时间。 */
        Instant detailCollectedAt,
        /** 来源系统返回的职位标签。 */
        List<String> sourceTags,
        /** 来源系统返回的技能标签。 */
        List<String> sourceSkillTags,
        /** 来源系统返回的福利标签。 */
        List<String> welfare) {}
