package com.informationplatform.hub.recommendation.api;

import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.common.api.BrowserApiErrorMessages;
import com.informationplatform.hub.common.logging.OperationalLogExceptions;
import com.informationplatform.hub.recommendation.feed.application.RecommendationFeedPersistenceException;
import com.informationplatform.hub.recommendation.feed.application.RecommendationFeedRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将 Feed 参数与持久化异常转换为稳定、脱敏的 API 响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RecommendationFeedController.class)
public class RecommendationFeedApiExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RecommendationFeedApiExceptionHandler.class);

    @ExceptionHandler(RecommendationFeedRequestException.class)
    ResponseEntity<ApiResponse<Void>> handleRequest(RecommendationFeedRequestException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(exception.getCode(), BrowserApiErrorMessages.message(exception.getCode())));
    }

    @ExceptionHandler(RecommendationFeedPersistenceException.class)
    ResponseEntity<ApiResponse<Void>> handlePersistence(
            RecommendationFeedPersistenceException exception) {
        LOGGER.error(
                "Recommendation Feed persistence operation failed",
                OperationalLogExceptions.sanitized(exception));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                                "RECOMMENDATION_FEED_PERSISTENCE_FAILED",
                                BrowserApiErrorMessages.message("RECOMMENDATION_FEED_PERSISTENCE_FAILED")));
    }
}
