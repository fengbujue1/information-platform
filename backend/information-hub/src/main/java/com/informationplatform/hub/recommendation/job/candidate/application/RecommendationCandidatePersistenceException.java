package com.informationplatform.hub.recommendation.job.candidate.application;

/** 表示 Candidate 查询结果违反已冻结持久化或 JSON 不变量。 */
public class RecommendationCandidatePersistenceException extends RuntimeException {

    public RecommendationCandidatePersistenceException(String message) {
        super(message);
    }

    public RecommendationCandidatePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
