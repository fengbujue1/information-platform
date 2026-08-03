package com.informationplatform.hub.analysis.prompt.application;

/** 表示当前 Owner 下不存在指定 Prompt 资源。 */
public class PromptNotFoundException extends RuntimeException {

    /** 稳定业务错误码。 */
    private final String code;

    public PromptNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
