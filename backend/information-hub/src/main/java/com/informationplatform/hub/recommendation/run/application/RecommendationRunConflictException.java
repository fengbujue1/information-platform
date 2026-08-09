package com.informationplatform.hub.recommendation.run.application;

/** 同一 Owner/Type 已有冲突 Manual Run 或请求状态发生并发变化。 */
public class RecommendationRunConflictException extends RuntimeException {

    private final String code;

    public RecommendationRunConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
