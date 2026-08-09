package com.informationplatform.hub.recommendation.domain;

/** 用户对 Information 的当前推荐反馈状态。 */
public enum FeedbackState {
    NONE,
    INTERESTED,
    NOT_INTERESTED;

    /** 将持久化/API 字符串转换为受控状态，拒绝数据库未约束的未知值。 */
    public static FeedbackState fromValue(String value) {
        try {
            return valueOf(value == null ? "" : value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported feedbackState: " + value, exception);
        }
    }
}
