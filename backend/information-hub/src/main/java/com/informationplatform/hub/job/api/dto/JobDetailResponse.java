package com.informationplatform.hub.job.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** 当前职位详情响应，只返回标准化数据，不暴露 rawPayload。 */
public record JobDetailResponse(
        /** 平台信息主键。 */
        long id,
        /** 信息来源。 */
        String source,
        /** 来源系统中的职位唯一标识。 */
        String sourceItemId,
        /** 来源职位详情页地址。 */
        String sourceUrl,
        /** 职位标题。 */
        String title,
        /** 职位正文。 */
        String content,
        /** 来源发布时间，未知时为空。 */
        Instant publishTime,
        /** 最近一次采集时间，UTC。 */
        Instant collectedAt,
        /** 平台首次发现时间，UTC。 */
        Instant firstSeenTime,
        /** 平台最近接收时间，UTC。 */
        Instant lastSeenTime,
        /** 当前标准化业务版本号。 */
        int currentVersionNo,
        /** 最近一次上报的采集器标识。 */
        String collectorId,
        /** 最近一次上报的采集器版本。 */
        String collectorVersion,
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
        /** 来源展示的薪资文本。 */
        String salaryText,
        /** 薪资信息来源或推导方式。 */
        String salarySource,
        /** 估算最低月薪，单位为人民币元。 */
        Integer salaryMinMonthlyYuan,
        /** 估算最高月薪，单位为人民币元。 */
        Integer salaryMaxMonthlyYuan,
        /** 每年发薪月数。 */
        Integer salaryMonths,
        /** 来源展示的完整工作地点。 */
        String locationName,
        /** 工作城市。 */
        String cityName,
        /** 工作行政区。 */
        String areaName,
        /** 工作商圈。 */
        String businessDistrictName,
        /** 工作经验要求。 */
        String experienceText,
        /** 学历要求。 */
        String educationText,
        /** 招聘者姓名。 */
        String recruiterName,
        /** 招聘者职位。 */
        String recruiterTitle,
        /** 最近一次被 Collector 观察到在线的来源文本。 */
        String recruiterActiveText,
        /** 办公方式。 */
        String remoteType,
        /** 来源职位状态。 */
        String jobStatus,
        /** 职位详情采集状态。 */
        String detailStatus,
        /** 职位详情采集或尝试时间，UTC。 */
        Instant detailCollectedAt,
        /** 来源职位标签 JSON 数组。 */
        JsonNode sourceTags,
        /** 来源技能标签 JSON 数组。 */
        JsonNode sourceSkillTags,
        /** 来源福利标签 JSON 数组。 */
        JsonNode welfare) {}
