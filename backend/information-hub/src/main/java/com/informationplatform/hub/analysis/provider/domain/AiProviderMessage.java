package com.informationplatform.hub.analysis.provider.domain;

public record AiProviderMessage(
        /** Chat 消息角色。 */
        AiProviderMessageRole role,
        /** 已由上层安全组装的消息原文。 */
        String content) {

    public AiProviderMessage {
        if (role == null) {
            throw new IllegalArgumentException("AI Provider message role must not be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("AI Provider message content must not be null");
        }
    }
}
