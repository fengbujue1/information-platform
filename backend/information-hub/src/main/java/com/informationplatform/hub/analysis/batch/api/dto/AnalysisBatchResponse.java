package com.informationplatform.hub.analysis.batch.api.dto;

import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchView;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Manual 与 Scheduled Analysis Batch 列表和详情响应。 */
public record AnalysisBatchResponse(
        /** Batch 主键。 */ long id,
        /** 触发类型。 */ String triggerType,
        /** Scheduled Batch 对应 Schedule 主键；Manual 时为空。 */ Long scheduleId,
        /** Scheduled Batch 的 UTC 计划点；Manual 时为空。 */ Instant scheduledFor,
        /** Prompt Profile 主键。 */ long promptProfileId,
        /** 冻结 Prompt Version 主键。 */ long promptVersionId,
        /** 信息类型。 */ String informationType,
        /** Definition Key。 */ String analysisDefinitionKey,
        /** Definition 版本。 */ int analysisDefinitionVersion,
        /** 候选窗口口径。 */ String windowBasis,
        /** 请求回看窗口天数。 */ int requestedWindowDays,
        /** UTC 窗口起点。 */ Instant windowStart,
        /** UTC 窗口终点。 */ Instant windowEnd,
        /** 请求 Candidate Limit。 */ int requestedMaxCandidates,
        /** 请求 Estimated Token Budget。 */ long requestedTokenBudget,
        /** 窗口内总数。 */ int totalInWindow,
        /** 未成功分析候选数。 */ int eligibleCount,
        /** Preview 时已成功分析数。 */ int alreadyAnalyzedCount,
        /** 预算内执行候选数。 */ int selectedCount,
        /** Candidate Limit 延后数。 */ int deferredByItemLimitCount,
        /** Token Budget 延后数。 */ int deferredByTokenBudgetCount,
        /** Estimated 输入 Token。 */ Long estimatedInputTokens,
        /** Estimated 输出 Token。 */ Long estimatedOutputTokens,
        /** Estimated 总 Token。 */ Long estimatedTotalTokens,
        /** Estimate 算法版本。 */ String estimateMethod,
        /** Batch 状态。 */ String status,
        /** NOOP 原因。 */ String skipReason,
        /** UTC 开始时间。 */ Instant startedAt,
        /** UTC 完成时间。 */ Instant completedAt,
        /** UTC 创建时间。 */ Instant createdAt,
        /** UTC 更新时间。 */ Instant updatedAt,
        /** 实时进度和 Actual Usage。 */ AnalysisBatchProgressResponse progress,
        /** Detail 返回 Items；列表与 Confirm 可为空。 */ List<AnalysisBatchItemResponse> items) {

    public AnalysisBatchResponse {
        items = List.copyOf(items);
    }

    public static AnalysisBatchResponse from(AnalysisBatchView view) {
        return new AnalysisBatchResponse(
                view.id(),
                view.triggerType(),
                view.scheduleId(),
                toInstant(view.scheduledFor()),
                view.promptProfileId(),
                view.promptVersionId(),
                view.informationType(),
                view.analysisDefinitionKey(),
                view.analysisDefinitionVersion(),
                view.windowBasis(),
                view.requestedWindowDays(),
                toInstant(view.windowStart()),
                toInstant(view.windowEnd()),
                view.requestedMaxCandidates(),
                view.requestedTokenBudget(),
                view.totalInWindow(),
                view.eligibleCount(),
                view.alreadyAnalyzedCount(),
                view.selectedCount(),
                view.deferredByItemLimitCount(),
                view.deferredByTokenBudgetCount(),
                view.estimatedInputTokens(),
                view.estimatedOutputTokens(),
                view.estimatedTotalTokens(),
                view.estimateMethod(),
                view.status(),
                view.skipReason(),
                toInstant(view.startedAt()),
                toInstant(view.completedAt()),
                toInstant(view.createdAt()),
                toInstant(view.updatedAt()),
                AnalysisBatchProgressResponse.from(view.progress()),
                view.items().stream().map(AnalysisBatchItemResponse::from).toList());
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
