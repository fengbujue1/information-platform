package com.informationplatform.hub.analysis.schedule.application;

/** 表示 Schedule 或 Scheduled Batch 持久化结果不符合预期。 */
public class AnalysisSchedulePersistenceException extends RuntimeException {

    public AnalysisSchedulePersistenceException(String message) {
        super(message);
    }
}
