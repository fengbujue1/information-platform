package com.informationplatform.hub.recommendation.application;

/** 表示 Interaction Contract 的状态、标识或领域类型不合法。 */
public class RecommendationInteractionRequestException extends RuntimeException {

    /** 对外稳定错误码。 */
    private final String code;

    public RecommendationInteractionRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
