package com.informationplatform.hub.analysis.processing.api;

import com.informationplatform.hub.analysis.processing.application.AnalysisConflictException;
import com.informationplatform.hub.analysis.processing.application.AnalysisNotFoundException;
import com.informationplatform.hub.analysis.processing.application.AnalysisPersistenceException;
import com.informationplatform.hub.analysis.processing.application.AnalysisRequestException;
import com.informationplatform.hub.analysis.provider.domain.AiProviderErrorType;
import com.informationplatform.hub.analysis.provider.domain.AiProviderException;
import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.common.api.BrowserApiErrorMessages;
import com.informationplatform.hub.common.logging.OperationalLogExceptions;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将 Analysis 业务、Provider 预检和持久化异常转换为稳定脱敏响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = InformationAnalysisController.class)
public class InformationAnalysisApiExceptionHandler {

    /** 日志不得包含 Prompt、Snapshot 正文、Provider 响应或秘密。 */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(InformationAnalysisApiExceptionHandler.class);

    @ExceptionHandler(AnalysisRequestException.class)
    ResponseEntity<ApiResponse<Void>> handleRequest(AnalysisRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_ANALYSIS_RESOURCE_ID",
                "Analysis resource id must be a positive integer");
    }

    @ExceptionHandler(AnalysisNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(AnalysisNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(AnalysisConflictException.class)
    ResponseEntity<ApiResponse<Void>> handleConflict(AnalysisConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(AiProviderException.class)
    ResponseEntity<ApiResponse<Void>> handleProviderPreflight(AiProviderException exception) {
        if (exception.errorType() == AiProviderErrorType.DISABLED
                || exception.errorType() == AiProviderErrorType.CONFIGURATION) {
            return error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_PROVIDER_" + exception.errorType().name(),
                    exception.getMessage());
        }
        return error(
                HttpStatus.BAD_GATEWAY,
                "AI_PROVIDER_UNAVAILABLE",
                "AI Provider is unavailable");
    }

    @ExceptionHandler({AnalysisPersistenceException.class, DataAccessException.class})
    ResponseEntity<ApiResponse<Void>> handlePersistence(RuntimeException exception) {
        // ERROR 保留堆栈；上游异常必须使用稳定脱敏消息。
        LOGGER.error(
                "Analysis persistence operation failed",
                OperationalLogExceptions.sanitized(exception));
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ANALYSIS_PERSISTENCE_FAILED",
                "Analysis data could not be persisted");
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, BrowserApiErrorMessages.message(code)));
    }
}
