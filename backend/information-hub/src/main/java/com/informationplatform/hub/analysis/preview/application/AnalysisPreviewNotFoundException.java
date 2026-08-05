package com.informationplatform.hub.analysis.preview.application;

/** Preview 所需 Owner 资源不存在或不可见。 */
public class AnalysisPreviewNotFoundException extends RuntimeException {

    /** 稳定 API 错误码。 */
    private final String code;

    public AnalysisPreviewNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
