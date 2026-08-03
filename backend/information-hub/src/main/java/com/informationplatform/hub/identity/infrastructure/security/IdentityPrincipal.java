package com.informationplatform.hub.identity.infrastructure.security;

import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Spring Security 使用的最小账号 Principal。 */
public final class IdentityPrincipal implements UserDetails {

    /** 用户账号主键。 */
    private final long userId;

    /** 规范化登录名。 */
    private final String username;

    /** 安全密码摘要。 */
    private final String passwordHash;

    /** 页面显示名。 */
    private final String displayName;

    /** 用户 IANA 时区。 */
    private final String timezone;

    /** 账号是否为 ACTIVE。 */
    private final boolean enabled;

    public IdentityPrincipal(
            long userId,
            String username,
            String passwordHash,
            String displayName,
            String timezone,
            boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.timezone = timezone;
        this.enabled = enabled;
    }

    public AuthenticatedUser toAuthenticatedUser() {
        return new AuthenticatedUser(userId, username, displayName, timezone);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
