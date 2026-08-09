package com.informationplatform.hub.common.logging;

/** 为可能携带 SQL 参数或业务正文的异常生成可定位但不含原消息的日志异常。 */
public final class OperationalLogExceptions {

    private OperationalLogExceptions() {
    }

    /** 保留异常类型和原始调用栈，主动丢弃 message、cause 与 suppressed 内容。 */
    public static RuntimeException sanitized(Throwable exception) {
        RuntimeException sanitized =
                new RuntimeException("Sanitized " + exception.getClass().getName());
        sanitized.setStackTrace(exception.getStackTrace());
        return sanitized;
    }
}
