package com.informationplatform.hub.identity.infrastructure.security;

import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** 从 Spring SecurityContext 解析可信当前用户。 */
@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public AuthenticatedUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof IdentityPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user is required");
        }
        return principal.toAuthenticatedUser();
    }
}
