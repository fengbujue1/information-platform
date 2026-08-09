package com.informationplatform.hub.recommendation.application;

/** 表示客户端可修正的 Recommendation Profile 请求错误。 */
public class RecommendationProfileRequestException extends RuntimeException {

    /** 稳定业务错误码。 */
    private final String code;

    public RecommendationProfileRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
