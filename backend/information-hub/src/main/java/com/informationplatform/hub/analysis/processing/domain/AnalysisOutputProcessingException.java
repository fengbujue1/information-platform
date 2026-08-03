package com.informationplatform.hub.analysis.processing.domain;

/** Provider 输出在进入 Definition Validator 前违反平台响应边界。 */
public class AnalysisOutputProcessingException extends RuntimeException {

    /** 供后续 Analysis 失败状态持久化使用的稳定错误码。 */
    private final String code;

    public AnalysisOutputProcessingException(String code, String message) {
        super(message);
        if (code == null || code.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Analysis output error metadata must not be blank");
        }
        this.code = code;
    }

    public String code() {
        return code;
    }
}
