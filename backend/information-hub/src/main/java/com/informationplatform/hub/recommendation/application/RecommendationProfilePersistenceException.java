package com.informationplatform.hub.recommendation.application;

/** 表示 Recommendation Profile 持久化结果违反应用层预期。 */
public class RecommendationProfilePersistenceException extends RuntimeException {

    public RecommendationProfilePersistenceException(String message) {
        super(message);
    }

    public RecommendationProfilePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
