package com.informationplatform.hub.analysis.prompt.application;

/** 表示 Prompt 唯一约束或状态冲突。 */
public class PromptConflictException extends RuntimeException {

    /** 稳定业务错误码。 */
    private final String code;

    public PromptConflictException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
