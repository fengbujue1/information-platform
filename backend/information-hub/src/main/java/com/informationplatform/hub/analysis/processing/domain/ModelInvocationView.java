package com.informationplatform.hub.analysis.processing.domain;

import java.time.LocalDateTime;

/** 面向当前 Owner 查询的一次模型调用审计视图。 */
public record ModelInvocationView(
        /** Invocation 主键。 */ long id,
        /** 同一 Analysis 内从 1 开始的尝试序号。 */ int attemptNo,
        /** Provider 稳定标识。 */ String provider,
        /** 实际或配置使用的模型名称。 */ String modelName,
        /** Provider 请求标识，缺失时为空。 */ String providerRequestId,
        /** RUNNING/SUCCEEDED/FAILED/TIMEOUT/UNKNOWN。 */ String status,
        /** Provider 完成原因，缺失时为空。 */ String finishReason,
        /** Provider 报告的实际输入 Token，未知时为空。 */ Long inputTokens,
        /** Provider 报告的实际输出 Token，未知时为空。 */ Long outputTokens,
        /** Provider 报告的实际总 Token，未知时为空。 */ Long totalTokens,
        /** Provider 报告的缓存输入 Token，未知时为空。 */ Long cachedInputTokens,
        /** Provider 报告的推理 Token，未知时为空。 */ Long reasoningTokens,
        /** REPORTED/UNAVAILABLE。 */ String usageStatus,
        /** 客户端观测耗时，单位毫秒。 */ Long latencyMs,
        /** 稳定脱敏错误码。 */ String errorCode,
        /** 脱敏错误摘要。 */ String errorMessage,
        /** UTC 开始时间。 */ LocalDateTime startedAt,
        /** UTC 完成时间。 */ LocalDateTime completedAt) {
}
