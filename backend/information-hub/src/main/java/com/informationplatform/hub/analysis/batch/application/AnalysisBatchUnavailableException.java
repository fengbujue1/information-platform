package com.informationplatform.hub.analysis.batch.application;

/** Worker 或 Provider 未配置时拒绝创建无法执行的 Batch。 */
public class AnalysisBatchUnavailableException extends RuntimeException {

    public AnalysisBatchUnavailableException(String message) {
        super(message);
    }
}
