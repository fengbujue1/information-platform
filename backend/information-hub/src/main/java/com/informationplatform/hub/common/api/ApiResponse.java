package com.informationplatform.hub.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        /** 请求是否处理成功。 */
        boolean success,
        /** 稳定的业务结果码。 */
        String code,
        /** 面向调用方的错误说明，成功时为空。 */
        String message,
        /** 成功时返回的业务数据。 */
        T data) {

    /** 构造不包含错误信息的成功响应。 */
    public static <T> ApiResponse<T> success(String code, T data) {
        return new ApiResponse<>(true, code, null, data);
    }

    /** 构造不包含业务数据的失败响应。 */
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
