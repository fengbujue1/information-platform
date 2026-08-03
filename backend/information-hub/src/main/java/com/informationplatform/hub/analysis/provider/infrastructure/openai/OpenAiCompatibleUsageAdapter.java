package com.informationplatform.hub.analysis.provider.infrastructure.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsage;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleUsageAdapter {

    /**
     * 映射 OpenAI-compatible Usage，保留 Provider 实际报告值且不补算任何 Token。
     *
     * <p>Usage 缺失或不含已知 Token 字段时返回 UNAVAILABLE；非法值拒绝进入 Actual Usage。
     */
    public AiProviderUsage adapt(JsonNode usage) {
        if (usage == null || usage.isNull()) {
            return AiProviderUsage.unavailable();
        }
        if (!usage.isObject()) {
            throw new IllegalArgumentException("Provider usage must be an object");
        }
        Long input = nullableNonNegativeLong(usage.get("prompt_tokens"));
        Long output = nullableNonNegativeLong(usage.get("completion_tokens"));
        Long total = nullableNonNegativeLong(usage.get("total_tokens"));
        Long cached = detailToken(usage, "prompt_tokens_details", "cached_tokens");
        Long reasoning = detailToken(usage, "completion_tokens_details", "reasoning_tokens");
        if (input == null && output == null && total == null && cached == null && reasoning == null) {
            return AiProviderUsage.unavailable();
        }
        return AiProviderUsage.reported(input, output, total, cached, reasoning);
    }

    private Long detailToken(JsonNode usage, String detailField, String tokenField) {
        JsonNode details = usage.get(detailField);
        if (details == null || details.isNull()) {
            return null;
        }
        if (!details.isObject()) {
            throw new IllegalArgumentException(detailField + " must be an object");
        }
        return nullableNonNegativeLong(details.get(tokenField));
    }

    private Long nullableNonNegativeLong(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
            throw new IllegalArgumentException("Provider usage token must be a non-negative integer");
        }
        return value.longValue();
    }
}
