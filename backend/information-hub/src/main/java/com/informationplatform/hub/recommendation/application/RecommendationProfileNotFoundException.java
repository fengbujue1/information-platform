package com.informationplatform.hub.recommendation.application;

/** 表示当前 Owner 下不存在指定 Information Type 的 Recommendation Profile。 */
public class RecommendationProfileNotFoundException extends RuntimeException {

    /** 稳定业务错误码。 */
    private final String code;

    public RecommendationProfileNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
