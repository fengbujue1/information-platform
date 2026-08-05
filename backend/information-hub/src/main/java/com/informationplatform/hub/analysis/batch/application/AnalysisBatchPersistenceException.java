package com.informationplatform.hub.analysis.batch.application;

/** Batch 持久化结果不符合预期。 */
public class AnalysisBatchPersistenceException extends RuntimeException {

    public AnalysisBatchPersistenceException(String message) {
        super(message);
    }
}
