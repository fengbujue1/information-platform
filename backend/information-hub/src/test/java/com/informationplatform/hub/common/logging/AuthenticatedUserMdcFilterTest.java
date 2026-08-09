package com.informationplatform.hub.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticatedUserMdcFilterTest {

    @AfterEach
    void clearContexts() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void exposesOnlyAuthenticatedSecurityUsernameAndCleansMdc() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "owner", "not-used", List.of()));
        AuthenticatedUserMdcFilter filter = new AuthenticatedUserMdcFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (servletRequest, servletResponse) ->
                        assertThat(MDC.get(OperationalLogContext.USERNAME))
                                .isEqualTo("owner"));

        assertThat(request.getAttribute(
                        OperationalLogContext.AUTHENTICATED_USERNAME_ATTRIBUTE))
                .isEqualTo("owner");
        assertThat(MDC.get(OperationalLogContext.USERNAME)).isNull();
    }

    @Test
    void systemRequestDoesNotInventUsername() throws Exception {
        AuthenticatedUserMdcFilter filter = new AuthenticatedUserMdcFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (servletRequest, servletResponse) ->
                        assertThat(MDC.get(OperationalLogContext.USERNAME)).isNull());

        assertThat(request.getAttribute(
                        OperationalLogContext.AUTHENTICATED_USERNAME_ATTRIBUTE))
                .isNull();
    }
}
