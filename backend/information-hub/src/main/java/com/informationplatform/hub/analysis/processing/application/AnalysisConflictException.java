package com.informationplatform.hub.analysis.processing.application;

/** Analysis 当前状态不允许普通重复执行。 */
public class AnalysisConflictException extends RuntimeException {
    /** 稳定 API 错误码。 */
    private final String code;

    public AnalysisConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
