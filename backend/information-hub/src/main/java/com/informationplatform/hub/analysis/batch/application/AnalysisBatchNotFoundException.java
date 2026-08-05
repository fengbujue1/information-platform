package com.informationplatform.hub.analysis.batch.application;

/** Batch 不存在或不属于当前 Owner。 */
public class AnalysisBatchNotFoundException extends RuntimeException {

    public AnalysisBatchNotFoundException(String message) {
        super(message);
    }
}
