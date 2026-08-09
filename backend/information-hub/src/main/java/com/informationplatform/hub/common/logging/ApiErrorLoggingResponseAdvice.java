package com.informationplatform.hub.common.logging;

import com.informationplatform.hub.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** 统一记录 Controller 层返回的 4xx validation 与业务拒绝，不记录请求体。 */
@RestControllerAdvice
public class ApiErrorLoggingResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ApiErrorLoggingResponseAdvice.class);

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        // ResponseEntity<ApiResponse<?>> 的声明类型不是 ApiResponse，需在写出阶段按实际 body 判断。
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        int status = response instanceof ServletServerHttpResponse servletResponse
                ? servletResponse.getServletResponse().getStatus()
                : 0;
        if (body instanceof ApiResponse<?> apiResponse
                && !apiResponse.success()
                && status >= 400
                && status < 500) {
            LOGGER.warn(
                    "Request rejected, path={}, status={}, errorCode={}, reason={}",
                    request.getURI().getPath(),
                    status,
                    apiResponse.code(),
                    apiResponse.message());
        }
        return body;
    }
}
