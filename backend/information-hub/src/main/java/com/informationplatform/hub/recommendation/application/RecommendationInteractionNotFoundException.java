package com.informationplatform.hub.recommendation.application;

/** 表示 Interaction 操作的目标 Information 不存在。 */
public class RecommendationInteractionNotFoundException extends RuntimeException {

    /** 对外稳定错误码。 */
    private final String code;

    public RecommendationInteractionNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
