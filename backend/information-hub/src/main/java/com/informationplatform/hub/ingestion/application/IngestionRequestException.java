package com.informationplatform.hub.ingestion.application;

public class IngestionRequestException extends RuntimeException {

    /** 返回给采集器的稳定业务错误码。 */
    private final String code;

    public IngestionRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
