package com.informationplatform.hub.recommendation.run.domain;

import java.time.LocalDateTime;

/** Owner 安全 Run 列表与详情共用的冻结输入、进度和终态视图。 */
public record RecommendationRunView(
        /** Recommendation Run 主键。 */ long id,
        /** Run 冻结的 Information Type。 */ String informationType,
        /** MANUAL 或 ANALYSIS_BATCH_COMPLETED。 */ String triggerType,
        /** 自动 Run 的来源 Analysis Batch；Manual 为空。 */ Long sourceAnalysisBatchId,
        /** 冻结 Recommendation Profile 主键。 */ long profileId,
        /** 冻结完整 Profile 的 SHA-256。 */ String profileContentHash,
        /** 冻结 AI Prompt Profile 主键。 */ long promptProfileId,
        /** 冻结不可变 Prompt Version 主键。 */ long promptVersionId,
        /** 推荐算法稳定 Key。 */ String algorithmKey,
        /** 推荐算法版本。 */ int algorithmVersion,
        /** UTC 候选窗口左闭起点。 */ LocalDateTime windowStart,
        /** UTC 候选窗口右开终点。 */ LocalDateTime windowEnd,
        /** 初始候选数量。 */ int candidateCount,
        /** hard exclusion 后候选数量。 */ int eligibleCount,
        /** 原子持久化 Item 数量。 */ int resultCount,
        /** PENDING/RUNNING/COMPLETED/FAILED/NOOP。 */ String status,
        /** NOOP 稳定原因。 */ String skipReason,
        /** FAILED 稳定错误码。 */ String failureCode,
        /** FAILED 非敏感诊断信息。 */ String failureMessage,
        /** UTC 开始时间。 */ LocalDateTime startedAt,
        /** UTC 终态时间。 */ LocalDateTime completedAt,
        /** UTC 创建时间。 */ LocalDateTime createdAt,
        /** UTC 更新时间。 */ LocalDateTime updatedAt) {
}
