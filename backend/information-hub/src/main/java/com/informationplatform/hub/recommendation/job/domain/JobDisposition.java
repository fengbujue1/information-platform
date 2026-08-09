package com.informationplatform.hub.recommendation.job.domain;

/** 用户在 JOB 求职流程中对职位的当前处理状态。 */
public enum JobDisposition {
    NONE,
    CONTACTED,
    CONTACTED_NOT_SUITABLE;

    /** 将持久化/API 字符串转换为受控 JOB 状态。 */
    public static JobDisposition fromValue(String value) {
        try {
            return valueOf(value == null ? "" : value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported jobDisposition: " + value, exception);
        }
    }

    /** 只有明确确认不合适的 JOB 状态属于领域 hard exclusion。 */
    public boolean isHardExclusion() {
        return this == CONTACTED_NOT_SUITABLE;
    }
}
