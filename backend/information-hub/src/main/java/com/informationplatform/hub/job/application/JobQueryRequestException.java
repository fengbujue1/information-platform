package com.informationplatform.hub.job.application;

/** 表示职位查询参数不符合分页、筛选或排序约束。 */
public class JobQueryRequestException extends RuntimeException {

    /** 返回给 API 调用方的稳定错误码。 */
    private final String code;

    public JobQueryRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
