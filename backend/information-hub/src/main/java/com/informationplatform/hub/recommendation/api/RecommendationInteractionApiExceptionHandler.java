package com.informationplatform.hub.recommendation.api;

import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.common.api.BrowserApiErrorMessages;
import com.informationplatform.hub.common.logging.OperationalLogExceptions;
import com.informationplatform.hub.recommendation.application.RecommendationInteractionNotFoundException;
import com.informationplatform.hub.recommendation.application.RecommendationInteractionPersistenceException;
import com.informationplatform.hub.recommendation.application.RecommendationInteractionRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将 Recommendation Interaction 业务与持久化异常转换为稳定响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RecommendationInteractionController.class)
public class RecommendationInteractionApiExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RecommendationInteractionApiExceptionHandler.class);

    @ExceptionHandler(RecommendationInteractionRequestException.class)
    ResponseEntity<ApiResponse<Void>> handleRequest(
            RecommendationInteractionRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(RecommendationInteractionNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(
            RecommendationInteractionNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({
        RecommendationInteractionPersistenceException.class,
        DataAccessException.class
    })
    ResponseEntity<ApiResponse<Void>> handlePersistence(RuntimeException exception) {
        LOGGER.error(
                "Recommendation Interaction persistence operation failed",
                OperationalLogExceptions.sanitized(exception));
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "RECOMMENDATION_INTERACTION_PERSISTENCE_FAILED",
                "Recommendation Interaction could not be persisted");
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, BrowserApiErrorMessages.message(code)));
    }
}
