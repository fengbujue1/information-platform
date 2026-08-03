package com.informationplatform.hub.identity.api.dto;

import com.informationplatform.hub.identity.domain.AuthenticatedUser;

/** 当前登录用户的安全公开投影。 */
public record CurrentUserResponse(
        /** 用户账号主键。 */
        long id,
        /** 规范化登录名。 */
        String username,
        /** 页面显示名，未配置时允许为空。 */
        String displayName,
        /** 用户的 IANA 时区。 */
        String timezone) {

    public static CurrentUserResponse from(AuthenticatedUser user) {
        return new CurrentUserResponse(
                user.id(), user.username(), user.displayName(), user.timezone());
    }
}
