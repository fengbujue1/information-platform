package com.informationplatform.hub.job.application;

/** 已完成边界校验和标准化的职位查询条件。 */
public record JobQueryCriteria(
        /** 从 1 开始的页码。 */
        int page,
        /** 每页记录数，最大为 100。 */
        int size,
        /** 数据库分页偏移量。 */
        long offset,
        /** 同时匹配标题、公司和正文的字面关键词。 */
        String keyword,
        /** 模糊匹配的公司名称。 */
        String company,
        /** 精确匹配的城市名称。 */
        String city,
        /** 查询区间最低月薪，单位为人民币元。 */
        Integer salaryMin,
        /** 查询区间最高月薪，单位为人民币元。 */
        Integer salaryMax,
        /** 精确匹配的信息来源。 */
        String source,
        /** 精确匹配的来源职位状态。 */
        String jobStatus,
        /** 精确匹配的办公方式。 */
        String remoteType,
        /** 已通过白名单校验的排序字段。 */
        JobSortField sortField,
        /** 已通过白名单校验的排序方向。 */
        SortDirection sortDirection) {}
