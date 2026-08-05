package com.informationplatform.hub.analysis.processing.api.dto;

import com.informationplatform.hub.analysis.processing.domain.ModelInvocationView;
import java.time.Instant;
import java.time.ZoneOffset;

/** 一次 Provider Invocation 与 Actual Usage 的安全响应。 */
public record ModelInvocationResponse(
        /** Invocation 主键。 */ long id,
        /** 同一 Analysis 内的尝试序号。 */ int attemptNo,
        /** Provider 稳定标识。 */ String provider,
        /** 模型名称。 */ String modelName,
        /** Provider 请求标识。 */ String providerRequestId,
        /** Invocation 状态。 */ String status,
        /** Provider 完成原因。 */ String finishReason,
        /** Actual 输入 Token。 */ Long inputTokens,
        /** Actual 输出 Token。 */ Long outputTokens,
        /** Actual 总 Token。 */ Long totalTokens,
        /** Actual 缓存输入 Token。 */ Long cachedInputTokens,
        /** Actual 推理 Token。 */ Long reasoningTokens,
        /** REPORTED/UNAVAILABLE。 */ String usageStatus,
        /** 客户端观测耗时，单位毫秒。 */ Long latencyMs,
        /** 稳定错误码。 */ String errorCode,
        /** 脱敏错误摘要。 */ String errorMessage,
        /** UTC 开始时间。 */ Instant startedAt,
        /** UTC 完成时间。 */ Instant completedAt) {

    public static ModelInvocationResponse from(ModelInvocationView view) {
        return new ModelInvocationResponse(
                view.id(), view.attemptNo(), view.provider(), view.modelName(),
                view.providerRequestId(), view.status(), view.finishReason(),
                view.inputTokens(), view.outputTokens(), view.totalTokens(),
                view.cachedInputTokens(), view.reasoningTokens(), view.usageStatus(),
                view.latencyMs(), view.errorCode(), view.errorMessage(),
                toInstant(view.startedAt()), toInstant(view.completedAt()));
    }

    private static Instant toInstant(java.time.LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
