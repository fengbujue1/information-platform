package com.informationplatform.hub.recommendation.run.application;

/** Recommendation Run API 参数或前置状态不合法。 */
public class RecommendationRunRequestException extends RuntimeException {

    private final String code;

    public RecommendationRunRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
