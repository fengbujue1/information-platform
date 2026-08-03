package com.informationplatform.hub.identity.api;

import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.identity.api.dto.CsrfTokenResponse;
import com.informationplatform.hub.identity.api.dto.CurrentUserResponse;
import com.informationplatform.hub.identity.api.dto.LoginRequest;
import com.informationplatform.hub.identity.application.CurrentUserProvider;
import com.informationplatform.hub.identity.application.IdentityAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供 Identity MVP 登录、当前用户和 CSRF 初始化协议。 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /** 账号认证与 Session 建立服务。 */
    private final IdentityAuthenticationService authenticationService;

    /** 当前可信用户提供者。 */
    private final CurrentUserProvider currentUserProvider;

    public AuthController(
            IdentityAuthenticationService authenticationService,
            CurrentUserProvider currentUserProvider) {
        this.authenticationService = authenticationService;
        this.currentUserProvider = currentUserProvider;
    }

    /** 校验账号密码并建立服务端 Session。 */
    @PostMapping("/login")
    public ApiResponse<CurrentUserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return ApiResponse.success(
                "LOGIN_SUCCEEDED",
                CurrentUserResponse.from(authenticationService.login(
                        request.username(),
                        request.password(),
                        servletRequest,
                        servletResponse)));
    }

    /** 返回当前 Session 对应的最小用户投影。 */
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.success(
                "CURRENT_USER_FOUND",
                CurrentUserResponse.from(currentUserProvider.requireCurrentUser()));
    }

    /** 激活并返回当前 Session 的 CSRF token。 */
    @GetMapping("/csrf")
    public ApiResponse<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ApiResponse.success(
                "CSRF_TOKEN_CREATED",
                new CsrfTokenResponse(
                        csrfToken.getHeaderName(),
                        csrfToken.getParameterName(),
                        csrfToken.getToken()));
    }
}
