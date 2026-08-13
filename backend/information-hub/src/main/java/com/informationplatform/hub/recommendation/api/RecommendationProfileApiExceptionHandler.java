package com.informationplatform.hub.recommendation.api;

import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.common.api.BrowserApiErrorMessages;
import com.informationplatform.hub.common.logging.OperationalLogExceptions;
import com.informationplatform.hub.recommendation.application.RecommendationProfileConflictException;
import com.informationplatform.hub.recommendation.application.RecommendationProfileNotFoundException;
import com.informationplatform.hub.recommendation.application.RecommendationProfilePersistenceException;
import com.informationplatform.hub.recommendation.application.RecommendationProfileRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将 Recommendation Profile 业务与持久化异常转换为稳定响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RecommendationProfileController.class)
public class RecommendationProfileApiExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RecommendationProfileApiExceptionHandler.class);

    @ExceptionHandler(RecommendationProfileRequestException.class)
    ResponseEntity<ApiResponse<Void>> handleRequest(
            RecommendationProfileRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(RecommendationProfileNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(
            RecommendationProfileNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(RecommendationProfileConflictException.class)
    ResponseEntity<ApiResponse<Void>> handleConflict(
            RecommendationProfileConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({RecommendationProfilePersistenceException.class, DataAccessException.class})
    ResponseEntity<ApiResponse<Void>> handlePersistence(RuntimeException exception) {
        LOGGER.error(
                "Recommendation Profile persistence operation failed",
                OperationalLogExceptions.sanitized(exception));
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "RECOMMENDATION_PROFILE_PERSISTENCE_FAILED",
                "Recommendation Profile could not be persisted");
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, BrowserApiErrorMessages.message(code)));
    }
}
