package com.informationplatform.hub.identity.domain;

/** 当前 Session 中可信的用户身份。 */
public record AuthenticatedUser(
        /** 用户账号主键，也是后续 Owner 隔离键。 */
        long id,
        /** 已执行 trim/lower 规范化的登录名。 */
        String username,
        /** 页面显示名，未配置时允许为空。 */
        String displayName,
        /** 用户的 IANA 时区名称。 */
        String timezone) {
}
