package com.informationplatform.hub.recommendation.domain;

import java.time.LocalDateTime;

/** 用户在一个 Information Type 下唯一的 Recommendation Profile 通用 Core。 */
public record RecommendationProfileCore(
        /** Profile Core 主键。 */ long id,
        /** Information Type；Phase 4 当前只支持 JOB。 */ String informationType,
        /** 绑定的同 Owner AI Prompt Profile 主键。 */ long analysisPromptProfileId,
        /** 推荐候选回看窗口天数，范围 1 至 30。 */ int windowDays,
        /** 每轮推荐最大结果数，范围 1 至 100。 */ int topN,
        /** Core 与领域扩展完整 canonical representation 的 SHA-256。 */ String contentHash,
        /** Profile 创建时间，按 UTC 保存。 */ LocalDateTime createdAt,
        /** Profile 最近更新时间，按 UTC 保存。 */ LocalDateTime updatedAt) {
}
