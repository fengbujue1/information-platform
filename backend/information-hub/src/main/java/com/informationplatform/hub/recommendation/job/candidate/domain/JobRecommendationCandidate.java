package com.informationplatform.hub.recommendation.job.candidate.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

/** 绑定当前 Snapshot 与最新兼容成功 Analysis 的 JOB Recommendation Candidate。 */
public record JobRecommendationCandidate(
        /** JOB Information 主键。 */
        long informationId,
        /** 解析时的当前不可变 Snapshot 主键。 */
        long snapshotId,
        /** 被消费的成功 USER_RELEVANCE Analysis 主键。 */
        long analysisId,
        /** 被消费的兼容 Analysis Definition 版本。 */
        int analysisDefinitionVersion,
        /** Information 首次进入平台的 UTC 时间。 */
        LocalDateTime firstSeenTime,
        /** 当前 Snapshot 标题。 */
        String title,
        /** 当前 Snapshot 正文，可为空。 */
        String content,
        /** 当前 Snapshot 的标准化通用与 JOB 事实。 */
        JsonNode standardizedPayload,
        /** 通过 Definition Schema 校验的不可变 Analysis 结果。 */
        JsonNode analysisResult,
        /** USER_RELEVANCE 相关度分数，范围 0..100。 */
        int aiRelevanceScore) {

    public JobRecommendationCandidate {
        if (informationId <= 0
                || snapshotId <= 0
                || analysisId <= 0
                || analysisDefinitionVersion <= 0
                || firstSeenTime == null
                || title == null
                || standardizedPayload == null
                || !standardizedPayload.isObject()
                || analysisResult == null
                || !analysisResult.isObject()
                || aiRelevanceScore < 0
                || aiRelevanceScore > 100) {
            throw new IllegalArgumentException("JOB Recommendation Candidate is invalid");
        }
        standardizedPayload = standardizedPayload.deepCopy();
        analysisResult = analysisResult.deepCopy();
    }

    @Override
    public JsonNode standardizedPayload() {
        return standardizedPayload.deepCopy();
    }

    @Override
    public JsonNode analysisResult() {
        return analysisResult.deepCopy();
    }
}
