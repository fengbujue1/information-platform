package com.informationplatform.hub.ingestion.api;

import java.io.IOException;

/** 实际读取的采集请求体超过配置上限时抛出的异常。 */
public class RequestSizeLimitExceededException extends IOException {

    /** 使用配置上限构造不包含请求内容的安全错误信息。 */
    public RequestSizeLimitExceededException(long maximumBytes) {
        super("Request body exceeds the configured limit of " + maximumBytes + " bytes");
    }
}
