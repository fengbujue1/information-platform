package com.informationplatform.hub.recommendation.job.ranking.domain;

import com.informationplatform.hub.recommendation.job.scoring.domain.JobRecommendationScoredCandidate;

/** TASK-039 为已评分 JOB Candidate 冻结的排名和确定性去重键。 */
public record JobRecommendationRankedCandidate(
        /** 保留 Candidate 来源事实、评分分项与推荐原因。 */
        JobRecommendationScoredCandidate scoredCandidate,
        /** 最终连续排名，从 1 开始。 */
        int rankNo,
        /** 公司、标题和城市规范化后计算的 SHA-256 十六进制去重键。 */
        String duplicateGroupKey) {

    public JobRecommendationRankedCandidate {
        if (scoredCandidate == null
                || rankNo <= 0
                || duplicateGroupKey == null
                || !duplicateGroupKey.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("JOB ranked candidate is invalid");
        }
    }
}
