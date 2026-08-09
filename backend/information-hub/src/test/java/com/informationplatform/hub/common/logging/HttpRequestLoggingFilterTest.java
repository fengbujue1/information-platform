package com.informationplatform.hub.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class HttpRequestLoggingFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void createsRequestIdLogsLifecycleAndClearsThreadContext(CapturedOutput output)
            throws Exception {
        HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get(OperationalLogContext.REQUEST_ID)).isNotBlank();
            assertThat(MDC.get(OperationalLogContext.USERNAME)).isNull();
            ((MockHttpServletResponse) servletResponse).setStatus(202);
        });

        String requestId = response.getHeader(OperationalLogContext.REQUEST_ID_HEADER);
        assertThat(requestId).isNotBlank();
        assertThat(output).contains(
                "HTTP request started, method=POST, path=/api/v1/test",
                "HTTP request completed, method=POST, path=/api/v1/test, status=202, durationMs=");
        assertThat(MDC.get(OperationalLogContext.REQUEST_ID)).isNull();
        assertThat(MDC.get(OperationalLogContext.USERNAME)).isNull();
    }
}
