package com.informationplatform.hub.common.logging;

/** 统一定义 HTTP 与异步日志上下文使用的稳定键名。 */
public final class OperationalLogContext {

    /** MDC 中的 HTTP 请求标识。 */
    public static final String REQUEST_ID = "requestId";

    /** MDC 中经过认证的登录名。 */
    public static final String USERNAME = "username";

    /** 返回给调用方的请求标识响应头。 */
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    /** Request 内部保存认证用户名的属性，不从客户端读取。 */
    static final String AUTHENTICATED_USERNAME_ATTRIBUTE =
            OperationalLogContext.class.getName() + ".authenticatedUsername";

    private OperationalLogContext() {
    }
}
