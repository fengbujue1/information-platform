package com.informationplatform.hub.analysis.processing.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.informationplatform.hub.analysis.processing.domain.InformationAnalysisView;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** 单条 Information Analysis 及 Invocation 的 Owner 安全响应。 */
public record InformationAnalysisResponse(
        /** Analysis 主键。 */ long id,
        /** Information 主键。 */ long informationId,
        /** 不可变 Snapshot 主键。 */ long snapshotId,
        /** 信息类型。 */ String informationType,
        /** Definition Key。 */ String analysisDefinitionKey,
        /** Definition 版本。 */ int analysisDefinitionVersion,
        /** 分析目的。 */ String analysisPurpose,
        /** Prompt Profile 主键。 */ long promptProfileId,
        /** 不可变 Prompt Version 主键。 */ long promptVersionId,
        /** Analysis 状态。 */ String status,
        /** 通过 Schema 校验的结构化结果。 */ JsonNode resultJson,
        /** JOB 相关度 0 至 100。 */ Integer relevanceScore,
        /** 页面摘要。 */ String summary,
        /** Estimated 输入 Token。 */ Long estimatedInputTokens,
        /** Estimated 输出 Token。 */ Long estimatedOutputTokens,
        /** Estimated 总 Token。 */ Long estimatedTotalTokens,
        /** Estimate 算法版本。 */ String estimateMethod,
        /** 稳定失败码。 */ String failureCode,
        /** 脱敏失败摘要。 */ String failureMessage,
        /** UTC 开始时间。 */ Instant startedAt,
        /** UTC 完成时间。 */ Instant completedAt,
        /** UTC 创建时间。 */ Instant createdAt,
        /** UTC 更新时间。 */ Instant updatedAt,
        /** 全部 Provider Invocation。 */ List<ModelInvocationResponse> invocations) {

    public InformationAnalysisResponse {
        resultJson = resultJson == null ? null : resultJson.deepCopy();
        invocations = List.copyOf(invocations);
    }

    public static InformationAnalysisResponse from(InformationAnalysisView view) {
        return new InformationAnalysisResponse(
                view.id(), view.informationId(), view.snapshotId(), view.informationType(),
                view.analysisDefinitionKey(), view.analysisDefinitionVersion(),
                view.analysisPurpose(), view.promptProfileId(), view.promptVersionId(),
                view.status(), view.resultJson(), view.relevanceScore(), view.summary(),
                view.estimatedInputTokens(), view.estimatedOutputTokens(),
                view.estimatedTotalTokens(), view.estimateMethod(), view.failureCode(),
                view.failureMessage(), toInstant(view.startedAt()), toInstant(view.completedAt()),
                toInstant(view.createdAt()), toInstant(view.updatedAt()),
                view.invocations().stream().map(ModelInvocationResponse::from).toList());
    }

    @Override
    public JsonNode resultJson() {
        return resultJson == null ? null : resultJson.deepCopy();
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
