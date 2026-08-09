package com.informationplatform.hub.recommendation.api.dto;

import com.informationplatform.hub.recommendation.run.domain.RecommendationRunView;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Recommendation Run 列表和详情响应，不返回完整 Profile snapshot 内容。 */
public record RecommendationRunResponse(
        /** Run 主键。 */ long id,
        /** Run 冻结 Information Type。 */ String informationType,
        /** MANUAL 或 ANALYSIS_BATCH_COMPLETED。 */ String triggerType,
        /** 自动 Run 来源 Batch；Manual 为空。 */ Long sourceAnalysisBatchId,
        /** 冻结 Profile 主键。 */ long profileId,
        /** 冻结完整 Profile SHA-256。 */ String profileContentHash,
        /** 冻结 Prompt Profile 主键。 */ long promptProfileId,
        /** 冻结 Prompt Version 主键。 */ long promptVersionId,
        /** 算法 Key。 */ String algorithmKey,
        /** 算法版本。 */ int algorithmVersion,
        /** UTC 窗口起点。 */ Instant windowStart,
        /** UTC 窗口终点。 */ Instant windowEnd,
        /** 初始候选数。 */ int candidateCount,
        /** hard exclusion 后候选数。 */ int eligibleCount,
        /** 最终 Item 数。 */ int resultCount,
        /** Run 状态。 */ String status,
        /** NOOP 原因。 */ String skipReason,
        /** FAILED 错误码。 */ String failureCode,
        /** FAILED 非敏感说明。 */ String failureMessage,
        /** UTC 开始时间。 */ Instant startedAt,
        /** UTC 终态时间。 */ Instant completedAt,
        /** UTC 创建时间。 */ Instant createdAt,
        /** UTC 更新时间。 */ Instant updatedAt) {

    public static RecommendationRunResponse from(RecommendationRunView view) {
        return new RecommendationRunResponse(
                view.id(),
                view.informationType(),
                view.triggerType(),
                view.sourceAnalysisBatchId(),
                view.profileId(),
                view.profileContentHash(),
                view.promptProfileId(),
                view.promptVersionId(),
                view.algorithmKey(),
                view.algorithmVersion(),
                toInstant(view.windowStart()),
                toInstant(view.windowEnd()),
                view.candidateCount(),
                view.eligibleCount(),
                view.resultCount(),
                view.status(),
                view.skipReason(),
                view.failureCode(),
                view.failureMessage(),
                toInstant(view.startedAt()),
                toInstant(view.completedAt()),
                toInstant(view.createdAt()),
                toInstant(view.updatedAt()));
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
