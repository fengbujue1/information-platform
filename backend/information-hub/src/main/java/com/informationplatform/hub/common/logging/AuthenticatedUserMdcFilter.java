package com.informationplatform.hub.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** 在 SecurityContext 加载后把可信认证用户名放入当前请求 MDC。 */
public class AuthenticatedUserMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authenticatedUsername(authentication);
        if (username != null) {
            MDC.put(OperationalLogContext.USERNAME, username);
            request.setAttribute(
                    OperationalLogContext.AUTHENTICATED_USERNAME_ATTRIBUTE, username);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(OperationalLogContext.USERNAME);
        }
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }
}
