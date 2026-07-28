package com.informationplatform.hub.job.application;

import java.util.Arrays;

/** 客户端允许选择的职位排序字段白名单。 */
public enum JobSortField {
    FIRST_SEEN_TIME("firstSeenTime"),
    LAST_SEEN_TIME("lastSeenTime"),
    PUBLISH_TIME("publishTime"),
    SALARY_MIN_MONTHLY_YUAN("salaryMinMonthlyYuan");

    /** API 中使用的排序字段名称。 */
    private final String externalName;

    JobSortField(String externalName) {
        this.externalName = externalName;
    }

    public String getExternalName() {
        return externalName;
    }

    /** 将外部字段名转换为固定枚举，禁止任意 SQL 排序列进入持久层。 */
    public static JobSortField fromExternalName(String value) {
        return Arrays.stream(values())
                .filter(field -> field.externalName.equals(value))
                .findFirst()
                .orElseThrow(() -> new JobQueryRequestException(
                        "INVALID_JOB_SORT_FIELD",
                        "sortBy must be one of firstSeenTime, lastSeenTime, "
                                + "publishTime, salaryMinMonthlyYuan"));
    }
}
