package com.informationplatform.hub.analysis.schedule.api;

import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewConfigurationException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewNotFoundException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewRequestException;
import com.informationplatform.hub.analysis.schedule.application.AnalysisScheduleConflictException;
import com.informationplatform.hub.analysis.schedule.application.AnalysisScheduleNotFoundException;
import com.informationplatform.hub.analysis.schedule.application.AnalysisSchedulePersistenceException;
import com.informationplatform.hub.analysis.schedule.application.AnalysisScheduleRequestException;
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

/** 将 Schedule 业务、Preview 和持久化异常转换为稳定脱敏响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AnalysisScheduleController.class)
public class AnalysisScheduleApiExceptionHandler {

    /** 日志不得包含 Prompt、Cookie、Token、Provider Secret 或 SQL 参数。 */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AnalysisScheduleApiExceptionHandler.class);

    @ExceptionHandler({
        AnalysisScheduleRequestException.class,
        AnalysisPreviewRequestException.class,
        ConstraintViolationException.class
    })
    ResponseEntity<ApiResponse<Void>> handleRequest(RuntimeException exception) {
        String code = exception instanceof AnalysisScheduleRequestException schedule
                ? schedule.getCode()
                : exception instanceof AnalysisPreviewRequestException preview
                        ? preview.getCode()
                        : "ANALYSIS_SCHEDULE_RESOURCE_ID_INVALID";
        return error(HttpStatus.BAD_REQUEST, code, exception.getMessage());
    }

    @ExceptionHandler({
        AnalysisScheduleNotFoundException.class,
        AnalysisPreviewNotFoundException.class
    })
    ResponseEntity<ApiResponse<Void>> handleNotFound(RuntimeException exception) {
        return error(
                HttpStatus.NOT_FOUND,
                "ANALYSIS_SCHEDULE_NOT_FOUND",
                "Analysis Schedule or Prompt Profile does not exist");
    }

    @ExceptionHandler(AnalysisScheduleConflictException.class)
    ResponseEntity<ApiResponse<Void>> handleConflict(AnalysisScheduleConflictException exception) {
        return error(
                HttpStatus.CONFLICT,
                "ANALYSIS_SCHEDULE_NAME_CONFLICT",
                exception.getMessage());
    }

    @ExceptionHandler(AnalysisPreviewConfigurationException.class)
    ResponseEntity<ApiResponse<Void>> handlePreviewConfiguration(
            AnalysisPreviewConfigurationException exception) {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ANALYSIS_PREVIEW_NOT_CONFIGURED",
                "Analysis Preview signing is not configured");
    }

    @ExceptionHandler({AnalysisSchedulePersistenceException.class, DataAccessException.class})
    ResponseEntity<ApiResponse<Void>> handlePersistence(RuntimeException exception) {
        LOGGER.error(
                "Analysis Schedule persistence operation failed",
                OperationalLogExceptions.sanitized(exception));
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ANALYSIS_SCHEDULE_PERSISTENCE_FAILED",
                "Analysis Schedule data could not be persisted");
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, BrowserApiErrorMessages.message(code)));
    }
}
