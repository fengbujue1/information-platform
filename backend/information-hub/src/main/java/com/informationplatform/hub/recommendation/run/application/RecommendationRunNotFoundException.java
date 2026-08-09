package com.informationplatform.hub.recommendation.run.application;

/** Run、Profile 或绑定 Prompt 在 Owner 边界内不存在。 */
public class RecommendationRunNotFoundException extends RuntimeException {

    private final String code;

    public RecommendationRunNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
