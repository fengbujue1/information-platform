package com.informationplatform.hub.analysis.definition.domain;

/** Definition 输入投影或模型输出违反冻结契约。 */
public class AnalysisDefinitionValidationException extends RuntimeException {

    /** 供调用方分类处理的稳定错误码。 */
    private final String code;

    public AnalysisDefinitionValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
