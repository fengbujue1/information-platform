package com.informationplatform.hub.analysis.prompt.application;

/** 表示客户端可修正的 Prompt 请求错误。 */
public class PromptRequestException extends RuntimeException {

    /** 稳定业务错误码。 */
    private final String code;

    public PromptRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
