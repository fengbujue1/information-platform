package com.informationplatform.hub.recommendation.run.application;

/** Recommendation Run/Item 读写未达到预期持久化结果。 */
public class RecommendationRunPersistenceException extends RuntimeException {

    public RecommendationRunPersistenceException(String message) {
        super(message);
    }

    public RecommendationRunPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
