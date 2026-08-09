package com.informationplatform.hub.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.informationplatform.hub.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class ApiErrorLoggingResponseAdviceTest {

    @Test
    void logsStableFourHundredRejectionWithoutRequestBody(CapturedOutput output) {
        ApiErrorLoggingResponseAdvice advice = new ApiErrorLoggingResponseAdvice();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/api/v1/test");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        servletResponse.setStatus(400);
        ApiResponse<Void> body =
                ApiResponse.error("VALIDATION_FAILED", "Invalid request field: title");

        Object returned = advice.beforeBodyWrite(
                body,
                null,
                MediaType.APPLICATION_JSON,
                null,
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse));

        assertThat(returned).isSameAs(body);
        assertThat(output).contains(
                "Request rejected, path=/api/v1/test, status=400, errorCode=VALIDATION_FAILED, reason=Invalid request field: title");
    }
}
