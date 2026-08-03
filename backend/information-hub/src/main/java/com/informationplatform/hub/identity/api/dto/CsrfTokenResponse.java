package com.informationplatform.hub.identity.api.dto;

/** Web 初始化状态修改请求所需的 CSRF token。 */
public record CsrfTokenResponse(
        /** 客户端提交 token 时使用的 HTTP Header 名。 */
        String headerName,
        /** 表单提交场景使用的参数名。 */
        String parameterName,
        /** 当前 Session 对应的 CSRF token。 */
        String token) {
}
