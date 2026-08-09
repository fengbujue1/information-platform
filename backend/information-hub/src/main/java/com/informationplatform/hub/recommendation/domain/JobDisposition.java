package com.informationplatform.hub.recommendation.domain;

/** 用户在真实求职流程中对职位的当前处理状态。 */
public enum JobDisposition {
    NONE,
    CONTACTED,
    CONTACTED_NOT_SUITABLE;

    /** 将持久化/API 字符串转换为受控状态，确保 CONTACTED 不被误作 hard exclusion。 */
    public static JobDisposition fromValue(String value) {
        try {
            return valueOf(value == null ? "" : value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported jobDisposition: " + value, exception);
        }
    }

    /** 只有明确确认不合适的状态属于 Job Disposition hard exclusion。 */
    public boolean isHardExclusion() {
        return this == CONTACTED_NOT_SUITABLE;
    }
}
