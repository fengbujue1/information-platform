package com.informationplatform.hub.analysis.processing.application;

/** Analysis 持久化或冻结 JSON 无法安全处理。 */
public class AnalysisPersistenceException extends RuntimeException {
    public AnalysisPersistenceException(String message) {
        super(message);
    }

    public AnalysisPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
