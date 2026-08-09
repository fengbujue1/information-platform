package com.informationplatform.hub.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 为每个 HTTP 请求建立 requestId，并记录统一的开始、完成和耗时日志。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    /** 包裹包括 Collector 与 Spring Security 在内的完整 HTTP 过滤器链。 */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long startedNanos = System.nanoTime();
        MDC.put(OperationalLogContext.REQUEST_ID, requestId);
        response.setHeader(OperationalLogContext.REQUEST_ID_HEADER, requestId);
        LOGGER.info(
                "HTTP request started, method={}, path={}",
                request.getMethod(),
                request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            LOGGER.error(
                    "HTTP request failed, method={}, path={}, durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    elapsedMillis(startedNanos),
                    exception);
            throw exception;
        } catch (Error error) {
            LOGGER.error(
                    "HTTP request failed, method={}, path={}, durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    elapsedMillis(startedNanos),
                    error);
            throw error;
        } finally {
            Object authenticatedUsername =
                    request.getAttribute(OperationalLogContext.AUTHENTICATED_USERNAME_ATTRIBUTE);
            if (authenticatedUsername instanceof String username && !username.isBlank()) {
                MDC.put(OperationalLogContext.USERNAME, username);
            }
            LOGGER.info(
                    "HTTP request completed, method={}, path={}, status={}, durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    elapsedMillis(startedNanos));
            // Servlet 容器线程会复用，必须显式移除，避免请求上下文串到后续请求。
            MDC.remove(OperationalLogContext.USERNAME);
            MDC.remove(OperationalLogContext.REQUEST_ID);
        }
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }
}
