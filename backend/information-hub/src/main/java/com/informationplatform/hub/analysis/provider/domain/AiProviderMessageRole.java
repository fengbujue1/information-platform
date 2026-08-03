package com.informationplatform.hub.analysis.provider.domain;

/** OpenAI-compatible Chat 消息角色。 */
public enum AiProviderMessageRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    /** Provider 协议使用的小写角色值。 */
    private final String wireValue;

    AiProviderMessageRole(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
