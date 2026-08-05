package com.informationplatform.hub.analysis.schedule.application;

/** 表示同一 Owner 下 Schedule 名称冲突。 */
public class AnalysisScheduleConflictException extends RuntimeException {

    public AnalysisScheduleConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
