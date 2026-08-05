package com.informationplatform.hub.analysis.preview.application;

/** Preview 签名秘密缺失或不满足安全要求。 */
public class AnalysisPreviewConfigurationException extends RuntimeException {

    public AnalysisPreviewConfigurationException(String message) {
        super(message);
    }
}
