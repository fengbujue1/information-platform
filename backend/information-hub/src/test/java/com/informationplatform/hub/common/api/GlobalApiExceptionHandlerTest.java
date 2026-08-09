package com.informationplatform.hub.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(OutputCaptureExtension.class)
class GlobalApiExceptionHandlerTest {

    @Test
    void unknownFailureReturnsStableResponseAndKeepsStackTrace(CapturedOutput output) {
        GlobalApiExceptionHandler handler = new GlobalApiExceptionHandler();
        IllegalStateException failure = new IllegalStateException("unexpected-marker");

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(failure);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(
                ApiResponse.error("INTERNAL_ERROR", "The request could not be processed"));
        assertThat(output).contains(
                "Unexpected Information Hub API failure",
                "java.lang.IllegalStateException: unexpected-marker");
    }
}
