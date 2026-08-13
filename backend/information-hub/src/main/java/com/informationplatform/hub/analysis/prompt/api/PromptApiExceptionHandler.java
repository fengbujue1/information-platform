package com.informationplatform.hub.analysis.prompt.api;

import com.informationplatform.hub.analysis.prompt.application.PromptConflictException;
import com.informationplatform.hub.analysis.prompt.application.PromptNotFoundException;
import com.informationplatform.hub.analysis.prompt.application.PromptPersistenceException;
import com.informationplatform.hub.analysis.prompt.application.PromptRequestException;
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

/** 将 Prompt API 业务与持久化异常转换为稳定且不泄露 Owner/SQL 的响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PromptProfileController.class)
public class PromptApiExceptionHandler {

    /** 日志禁止记录 User Prompt 正文或请求体。 */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PromptApiExceptionHandler.class);

    @ExceptionHandler(PromptRequestException.class)
    ResponseEntity<ApiResponse<Void>> handleRequest(PromptRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_PROMPT_RESOURCE_ID",
                "Prompt resource id must be a positive integer");
    }

    @ExceptionHandler(PromptNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(PromptNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(PromptConflictException.class)
    ResponseEntity<ApiResponse<Void>> handleConflict(PromptConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({PromptPersistenceException.class, DataAccessException.class})
    ResponseEntity<ApiResponse<Void>> handlePersistence(RuntimeException exception) {
        // ERROR 保留堆栈；业务代码和 Mapper 不得把 User Prompt 正文拼入异常消息。
        LOGGER.error(
                "Prompt persistence operation failed",
                OperationalLogExceptions.sanitized(exception));
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PROMPT_PERSISTENCE_FAILED",
                "Prompt data could not be persisted");
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, BrowserApiErrorMessages.message(code)));
    }
}
