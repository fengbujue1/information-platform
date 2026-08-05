package com.informationplatform.hub.analysis.schedule.application;

/** 表示 Schedule 不存在或不属于当前 Owner。 */
public class AnalysisScheduleNotFoundException extends RuntimeException {

    public AnalysisScheduleNotFoundException(String message) {
        super(message);
    }
}
