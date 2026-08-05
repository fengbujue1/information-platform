package com.informationplatform.hub.analysis.processing.application;

/** 单条 Analysis 请求违反业务前置条件。 */
public class AnalysisRequestException extends RuntimeException {
    /** 稳定 API 错误码。 */
    private final String code;

    public AnalysisRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
