package com.informationplatform.hub.recommendation.application;

/** 隐藏 Interaction 持久化不变量失败的内部细节。 */
public class RecommendationInteractionPersistenceException extends RuntimeException {

    public RecommendationInteractionPersistenceException(String message) {
        super(message);
    }
}
