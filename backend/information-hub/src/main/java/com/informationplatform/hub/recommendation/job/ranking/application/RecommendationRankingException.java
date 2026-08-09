package com.informationplatform.hub.recommendation.job.ranking.application;

/** Candidate 缺少生成稳定 JOB 去重或多样性分组所需的结构化事实。 */
public class RecommendationRankingException extends RuntimeException {

    public RecommendationRankingException(String message) {
        super(message);
    }
}
