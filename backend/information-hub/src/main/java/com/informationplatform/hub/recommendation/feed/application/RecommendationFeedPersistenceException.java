package com.informationplatform.hub.recommendation.feed.application;

/** Recommendation Feed 读取或持久化 JSON 事实无法安全解析。 */
public class RecommendationFeedPersistenceException extends RuntimeException {

    public RecommendationFeedPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
