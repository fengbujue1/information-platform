package com.informationplatform.hub.analysis.preview.api;

import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewConfigurationException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewConflictException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewNotFoundException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewRequestException;
import com.informationplatform.hub.analysis.processing.application.AnalysisPersistenceException;
import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.common.logging.OperationalLogExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将 Preview 业务、签名配置和持久化异常转换为稳定脱敏响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AnalysisPreviewController.class)
public class AnalysisPreviewApiExceptionHandler {

    /** 日志不得包含 Prompt、候选正文、Token 或 HMAC Secret。 */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AnalysisPreviewApiExceptionHandler.class);

    @ExceptionHandler(AnalysisPreviewRequestException.class)
    ResponseEntity<ApiResponse<Void>> handleRequest(AnalysisPreviewRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(AnalysisPreviewNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(AnalysisPreviewNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(AnalysisPreviewConflictException.class)
    ResponseEntity<ApiResponse<Void>> handleConflict(AnalysisPreviewConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(AnalysisPreviewConfigurationException.class)
    ResponseEntity<ApiResponse<Void>> handleConfiguration(
            AnalysisPreviewConfigurationException exception) {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ANALYSIS_PREVIEW_NOT_CONFIGURED",
                "Analysis Preview signing is not configured");
    }

    @ExceptionHandler({AnalysisPersistenceException.class, DataAccessException.class})
    ResponseEntity<ApiResponse<Void>> handlePersistence(RuntimeException exception) {
        // ERROR 保留堆栈；业务异常消息不得携带 Prompt、候选正文或 Token。
        LOGGER.error(
                "Analysis Preview persistence operation failed",
                OperationalLogExceptions.sanitized(exception));
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ANALYSIS_PREVIEW_PERSISTENCE_FAILED",
                "Analysis Preview data could not be read");
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}
