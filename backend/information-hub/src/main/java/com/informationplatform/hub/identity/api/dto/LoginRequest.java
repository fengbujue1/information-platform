package com.informationplatform.hub.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Identity MVP 登录请求。 */
public record LoginRequest(
        /** 登录用户名，服务端会执行 trim/lower 规范化。 */
        @NotBlank @Size(max = 100) String username,
        /** 登录明文密码，仅用于本次认证，不得落库或写日志。 */
        @NotBlank @Size(max = 1024) String password) {
}
