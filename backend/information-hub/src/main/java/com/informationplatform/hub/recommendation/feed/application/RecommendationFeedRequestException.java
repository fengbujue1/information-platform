package com.informationplatform.hub.recommendation.feed.application;

/** Recommendation Feed 请求参数不符合公开 Contract。 */
public class RecommendationFeedRequestException extends RuntimeException {

    /** 对外稳定错误码。 */
    private final String code;

    public RecommendationFeedRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
