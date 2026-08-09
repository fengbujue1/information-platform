package com.informationplatform.hub.recommendation.job.scoring.application;

/** Candidate 的冻结标准化事实无法按 JOB_RECOMMENDATION V1 解释。 */
public class RecommendationScoringException extends RuntimeException {

    public RecommendationScoringException(String message) {
        super(message);
    }
}
