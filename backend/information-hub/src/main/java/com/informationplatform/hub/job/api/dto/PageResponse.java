package com.informationplatform.hub.job.api.dto;

import java.util.List;

/** 通用分页响应，不包含数据库实现细节。 */
public record PageResponse<T>(
        /** 当前页码，从 1 开始。 */
        int page,
        /** 当前请求的每页记录数。 */
        int size,
        /** 符合筛选条件的总记录数。 */
        long total,
        /** 按当前每页记录数计算的总页数。 */
        long totalPages,
        /** 当前页的业务数据。 */
        List<T> items) {}
