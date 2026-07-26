package com.informationplatform.hub.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorWriter {

    /** 用于将统一错误响应序列化为 JSON。 */
    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 在过滤器阶段直接写出统一 JSON 错误响应。 */
    public void write(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 过滤器异常无法进入 ControllerAdvice，因此在此复用统一响应结构。
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(code, message));
    }
}
