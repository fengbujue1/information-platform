package com.informationplatform.hub.analysis.prompt.application;

/** 表示 Prompt 持久化结果违反应用层预期。 */
public class PromptPersistenceException extends RuntimeException {

    public PromptPersistenceException(String message) {
        super(message);
    }
}
