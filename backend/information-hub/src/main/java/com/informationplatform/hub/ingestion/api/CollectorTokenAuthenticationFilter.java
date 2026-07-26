package com.informationplatform.hub.ingestion.api;

import com.informationplatform.hub.common.api.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CollectorTokenAuthenticationFilter extends OncePerRequestFilter {

    /** 第一阶段采集器写入接口路径。 */
    private static final String COLLECTOR_ITEMS_PATH = "/api/v1/collector/items";

    /** Authorization 请求头中的 Bearer 前缀。 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 采集接口认证配置。 */
    private final CollectorApiProperties properties;

    /** 用于在过滤器阶段输出统一错误响应。 */
    private final ApiErrorWriter errorWriter;

    public CollectorTokenAuthenticationFilter(
            CollectorApiProperties properties, ApiErrorWriter errorWriter) {
        this.properties = properties;
        this.errorWriter = errorWriter;
    }

    /** 仅对采集器写入接口执行 Token 认证。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !COLLECTOR_ITEMS_PATH.equals(request.getRequestURI());
    }

    /** 校验服务端 Token 配置和请求中的 Bearer Token。 */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String configuredToken = properties.getToken();
        // 未配置 Token 时关闭写入入口，避免误以为接口处于受保护状态。
        if (configuredToken.isBlank()) {
            errorWriter.write(
                    response,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "COLLECTOR_AUTH_NOT_CONFIGURED",
                    "Collector authentication is not configured");
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        // 认证失败时不记录请求 Token，防止敏感凭据进入日志。
        if (!isValidBearerToken(authorization, configuredToken)) {
            errorWriter.write(
                    response,
                    HttpStatus.UNAUTHORIZED.value(),
                    "COLLECTOR_AUTHENTICATION_FAILED",
                    "A valid collector bearer token is required");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** 提取 Bearer Token，并以恒定时间比较降低时序侧信道风险。 */
    private boolean isValidBearerToken(String authorization, String configuredToken) {
        if (authorization == null
                || authorization.length() <= BEARER_PREFIX.length()
                || !authorization.regionMatches(
                        true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return false;
        }
        String providedToken = authorization.substring(BEARER_PREFIX.length());
        // MessageDigest.isEqual 避免普通字符串比较过早返回。
        return MessageDigest.isEqual(
                providedToken.getBytes(StandardCharsets.UTF_8),
                configuredToken.getBytes(StandardCharsets.UTF_8));
    }
}
