package com.informationplatform.hub.recommendation.application;

/** 表示 Recommendation Profile 并发创建产生的唯一键冲突。 */
public class RecommendationProfileConflictException extends RuntimeException {

    /** 稳定业务错误码。 */
    private final String code;

    public RecommendationProfileConflictException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
