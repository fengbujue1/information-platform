package com.informationplatform.hub.analysis.batch.api;

import com.informationplatform.hub.analysis.batch.application.AnalysisBatchNotFoundException;
import com.informationplatform.hub.analysis.batch.application.AnalysisBatchPersistenceException;
import com.informationplatform.hub.analysis.batch.application.AnalysisBatchUnavailableException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewConfigurationException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewConflictException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewNotFoundException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewRequestException;
import com.informationplatform.hub.analysis.provider.domain.AiProviderException;
import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.common.api.BrowserApiErrorMessages;
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

/** 将 Batch 业务、Preview 和持久化异常转换为稳定脱敏响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AnalysisBatchController.class)
public class AnalysisBatchApiExceptionHandler {

    /** 记录脱敏堆栈，不记录 Token、Prompt、候选或 Provider 秘密。 */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AnalysisBatchApiExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, AnalysisPreviewRequestException.class})
    ResponseEntity<ApiResponse<Void>> handleRequest(RuntimeException exception) {
        String code = exception instanceof AnalysisPreviewRequestException preview
                ? preview.getCode() : "ANALYSIS_BATCH_REQUEST_INVALID";
        return error(HttpStatus.BAD_REQUEST, code, exception.getMessage());
    }

    @ExceptionHandler({AnalysisBatchNotFoundException.class, AnalysisPreviewNotFoundException.class})
    ResponseEntity<ApiResponse<Void>> handleNotFound(RuntimeException exception) {
        return error(
                HttpStatus.NOT_FOUND,
                "ANALYSIS_BATCH_NOT_FOUND",
                "Analysis Batch does not exist");
    }

    @ExceptionHandler(AnalysisPreviewConflictException.class)
    ResponseEntity<ApiResponse<Void>> handleConflict(AnalysisPreviewConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({
        AnalysisPreviewConfigurationException.class,
        AnalysisBatchUnavailableException.class,
        AiProviderException.class
    })
    ResponseEntity<ApiResponse<Void>> handleUnavailable(RuntimeException exception) {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ANALYSIS_BATCH_UNAVAILABLE",
                "Analysis Batch execution is not configured");
    }

    @ExceptionHandler({AnalysisBatchPersistenceException.class, DataAccessException.class})
    ResponseEntity<ApiResponse<Void>> handlePersistence(RuntimeException exception) {
        LOGGER.error(
                "Analysis Batch persistence operation failed",
                OperationalLogExceptions.sanitized(exception));
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ANALYSIS_BATCH_PERSISTENCE_FAILED",
                "Analysis Batch data could not be persisted");
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, BrowserApiErrorMessages.message(code)));
    }
}
