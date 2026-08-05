package com.informationplatform.hub.analysis.processing.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

/** 单条 Information Analysis 及其 Invocation 审计视图。 */
public record InformationAnalysisView(
        /** Analysis 主键。 */ long id,
        /** 被分析的 Information 主键。 */ long informationId,
        /** 被分析的不可变 Snapshot 主键。 */ long snapshotId,
        /** 信息类型，当前为 JOB。 */ String informationType,
        /** Analysis Definition Key。 */ String analysisDefinitionKey,
        /** Analysis Definition 版本。 */ int analysisDefinitionVersion,
        /** 分析目的，当前为 USER_RELEVANCE。 */ String analysisPurpose,
        /** Prompt Profile 主键。 */ long promptProfileId,
        /** 不可变 Prompt Version 主键。 */ long promptVersionId,
        /** PENDING/RUNNING/SUCCEEDED/FAILED。 */ String status,
        /** 通过严格 Schema 校验的结果，未成功时为空。 */ JsonNode resultJson,
        /** JOB 相关度 0 至 100，未成功时为空。 */ Integer relevanceScore,
        /** 页面摘要，未成功时为空。 */ String summary,
        /** 调用前预估输入 Token。 */ Long estimatedInputTokens,
        /** 调用前预估输出 Token。 */ Long estimatedOutputTokens,
        /** 调用前预估总 Token。 */ Long estimatedTotalTokens,
        /** 估算算法版本。 */ String estimateMethod,
        /** 稳定失败码。 */ String failureCode,
        /** 脱敏失败摘要。 */ String failureMessage,
        /** UTC 开始时间。 */ LocalDateTime startedAt,
        /** UTC 完成时间。 */ LocalDateTime completedAt,
        /** UTC 创建时间。 */ LocalDateTime createdAt,
        /** UTC 更新时间。 */ LocalDateTime updatedAt,
        /** 按 attemptNo 递增的 Invocation。 */ List<ModelInvocationView> invocations) {

    public InformationAnalysisView {
        resultJson = resultJson == null ? null : resultJson.deepCopy();
        invocations = List.copyOf(invocations);
    }

    @Override
    public JsonNode resultJson() {
        return resultJson == null ? null : resultJson.deepCopy();
    }
}
