package com.informationplatform.hub.analysis.schedule.application;

/** 表示 Schedule 请求参数或配置不满足冻结约束。 */
public class AnalysisScheduleRequestException extends RuntimeException {

    /** 对外稳定错误码。 */
    private final String code;

    public AnalysisScheduleRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
