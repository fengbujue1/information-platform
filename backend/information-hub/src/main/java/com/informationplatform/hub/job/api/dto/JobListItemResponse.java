package com.informationplatform.hub.job.api.dto;

import java.time.Instant;

/** 职位列表中的轻量摘要，明确排除正文和 rawPayload。 */
public record JobListItemResponse(
        /** 平台信息主键。 */
        long id,
        /** 信息来源，例如 BOSS。 */
        String source,
        /** 来源系统中的职位唯一标识。 */
        String sourceItemId,
        /** 来源职位详情页地址。 */
        String sourceUrl,
        /** 职位标题。 */
        String title,
        /** 公司名称。 */
        String companyName,
        /** 来源展示的薪资文本。 */
        String salaryText,
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
        /** 工作经验要求。 */
        String experienceText,
        /** 学历要求。 */
        String educationText,
        /** 办公方式。 */
        String remoteType,
        /** 来源职位状态。 */
        String jobStatus,
        /** 来源发布时间，未知时为空。 */
        Instant publishTime,
        /** 平台首次发现时间，UTC。 */
        Instant firstSeenTime,
        /** 平台最近接收时间，UTC。 */
        Instant lastSeenTime,
        /** 当前标准化业务版本号。 */
        int currentVersionNo) {}
