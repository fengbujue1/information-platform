package com.informationplatform.hub.analysis.preview.application;

/** Preview Token 过期、篡改或与确认上下文冲突。 */
public class AnalysisPreviewConflictException extends RuntimeException {

    /** 稳定 API 错误码。 */
    private final String code;

    public AnalysisPreviewConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
