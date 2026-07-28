package com.informationplatform.hub.job.application;

import java.util.Locale;

/** 查询排序方向白名单。 */
public enum SortDirection {
    ASC,
    DESC;

    public boolean isAscending() {
        return this == ASC;
    }

    /** 将外部排序方向转换为固定枚举。 */
    public static SortDirection fromExternalName(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new JobQueryRequestException(
                    "INVALID_JOB_SORT_DIRECTION",
                    "sortDirection must be asc or desc");
        }
    }
}
