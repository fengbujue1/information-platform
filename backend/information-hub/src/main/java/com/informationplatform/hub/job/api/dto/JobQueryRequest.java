package com.informationplatform.hub.job.api.dto;

/**
 * 职位分页查询参数。
 *
 * <p>参数的默认值、边界和排序白名单由应用服务统一校验，Controller 只负责协议绑定。
 */
public record JobQueryRequest(
        /** 从 1 开始的页码，缺省时为 1。 */
        Integer page,
        /** 每页记录数，缺省时为 20，最大为 100。 */
        Integer size,
        /** 同时匹配职位标题、公司名称和职位正文的关键词。 */
        String keyword,
        /** 按名称模糊匹配的公司筛选条件。 */
        String company,
        /** 按名称精确匹配的城市筛选条件。 */
        String city,
        /** 查询薪资区间的最低月薪，单位为人民币元。 */
        Integer salaryMin,
        /** 查询薪资区间的最高月薪，单位为人民币元。 */
        Integer salaryMax,
        /** 信息来源精确筛选条件，例如 BOSS。 */
        String source,
        /** 来源职位状态精确筛选条件。 */
        String jobStatus,
        /** 办公方式精确筛选条件。 */
        String remoteType,
        /** 排序字段，缺省时为 firstSeenTime。 */
        String sortBy,
        /** 排序方向，可选 asc 或 desc，缺省时为 desc。 */
        String sortDirection) {}
