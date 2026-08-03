package com.informationplatform.hub.identity.domain;

import java.util.Locale;

/** 统一账号登录名的应用层唯一语义。 */
public final class UsernameNormalizer {

    private UsernameNormalizer() {
    }

    /** 对登录名执行去除首尾空白和与语言无关的小写规范化。 */
    public static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
