package com.informationplatform.hub.common.api;

import com.informationplatform.hub.ingestion.api.RequestSizeLimitExceededException;
import com.informationplatform.hub.ingestion.application.IngestionRequestException;
import com.informationplatform.hub.job.application.JobNotFoundException;
import com.informationplatform.hub.job.application.JobQueryPersistenceException;
import com.informationplatform.hub.job.application.JobQueryRequestException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    /** 仅记录服务端异常，不将堆栈或敏感数据返回给调用方。 */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    /** 将 Bean Validation 错误转换为稳定的参数错误响应。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        // 只返回第一个错误字段，避免把内部校验细节和完整请求内容暴露给调用方。
        String field = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField())
                .orElse("request");
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Invalid request field: " + field);
    }

    /** 区分请求体超限与普通 JSON 解析失败。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException exception) {
        // 请求体包装流抛出的超限异常可能被 Jackson 包装，需要沿异常链识别。
        if (hasCause(exception, RequestSizeLimitExceededException.class)) {
            return error(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "REQUEST_TOO_LARGE",
                    "Request body exceeds the configured size limit");
        }
        return error(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Request body is not valid JSON");
    }

    /** 将接入层主动拒绝的业务请求转换为 400 响应。 */
    @ExceptionHandler(IngestionRequestException.class)
    ResponseEntity<ApiResponse<Void>> handleIngestionRequest(IngestionRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getCode(), exception.getMessage());
    }

    /** 将职位分页、筛选和排序校验错误转换为稳定的 400 响应。 */
    @ExceptionHandler(JobQueryRequestException.class)
    ResponseEntity<ApiResponse<Void>> handleJobQueryRequest(
            JobQueryRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getCode(), exception.getMessage());
    }

    /** 将路径参数类型错误转换为稳定的 400 响应。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_PARAMETER",
                "Request parameter has an invalid type");
    }

    /** 对不存在的职位返回 404，不暴露数据库查询细节。 */
    @ExceptionHandler(JobNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleJobNotFound(JobNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Job does not exist");
    }

    /** 隐藏职位查询中的数据库或持久化 JSON 异常。 */
    @ExceptionHandler(JobQueryPersistenceException.class)
    ResponseEntity<ApiResponse<Void>> handleJobQueryPersistence(
            JobQueryPersistenceException exception) {
        LOGGER.error("Job query persistence operation failed", exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "JOB_QUERY_FAILED",
                "Job information could not be queried");
    }

    /** 隐藏数据库异常细节并返回稳定错误码。 */
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException exception) {
        LOGGER.error("Information ingestion database operation failed", exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INGESTION_PERSISTENCE_FAILED",
                "Information could not be persisted");
    }

    /** 兜底处理未预期异常，避免向客户端暴露内部实现。 */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected Information Hub API failure", exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "The request could not be processed");
    }

    /** 沿异常链查找指定类型，用于识别被框架包装的根因。 */
    private boolean hasCause(Throwable throwable, Class<? extends Throwable> expectedType) {
        Throwable current = throwable;
        while (Objects.nonNull(current)) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /** 创建统一结构的 HTTP 错误响应。 */
    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}
