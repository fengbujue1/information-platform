package com.informationplatform.hub.analysis.provider.domain;

import java.util.List;

public record AiProviderRequest(
        /** 已按顺序组装的 Chat 消息。 */
        List<AiProviderMessage> messages,
        /** 本次允许的最大输出 Token，范围 1 至 1000。 */
        int maxOutputTokens) {

    public AiProviderRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("AI Provider messages must not be empty");
        }
        messages = List.copyOf(messages);
        if (maxOutputTokens <= 0 || maxOutputTokens > 1000) {
            throw new IllegalArgumentException("maxOutputTokens must be between 1 and 1000");
        }
    }
}
