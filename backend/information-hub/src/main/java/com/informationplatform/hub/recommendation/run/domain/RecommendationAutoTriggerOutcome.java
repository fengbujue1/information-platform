package com.informationplatform.hub.recommendation.run.domain;

/** Analysis Batch Auto Trigger 的创建或稳定跳过结果。 */
public record RecommendationAutoTriggerOutcome(
        /** 是否新建了 Recommendation Run。 */ boolean created,
        /** 新建或已存在的 Run 主键；无关联 Run 时为空。 */ Long runId,
        /** 未创建时的稳定跳过原因；创建成功时为空。 */ String skipReason) {

    public static RecommendationAutoTriggerOutcome created(long runId) {
        return new RecommendationAutoTriggerOutcome(true, runId, null);
    }

    public static RecommendationAutoTriggerOutcome skipped(String reason) {
        return new RecommendationAutoTriggerOutcome(false, null, reason);
    }

    public static RecommendationAutoTriggerOutcome duplicate(long runId) {
        return new RecommendationAutoTriggerOutcome(false, runId, "SOURCE_BATCH_ALREADY_TRIGGERED");
    }
}
