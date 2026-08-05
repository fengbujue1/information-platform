package com.informationplatform.hub.analysis.preview.application;

/** Preview 请求不符合冻结边界。 */
public class AnalysisPreviewRequestException extends RuntimeException {

    /** 稳定 API 错误码。 */
    private final String code;

    public AnalysisPreviewRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
