package com.informationplatform.hub.analysis.processing.application;

/** Analysis、Snapshot 或 Prompt 不存在，且越权场景使用相同语义。 */
public class AnalysisNotFoundException extends RuntimeException {
    /** 稳定 API 错误码。 */
    private final String code;

    public AnalysisNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
