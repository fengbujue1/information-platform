package com.informationplatform.hub.identity.application;

import com.informationplatform.hub.identity.domain.AuthenticatedUser;
import com.informationplatform.hub.identity.domain.UsernameNormalizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/** 负责账号认证并建立防 Session Fixation 的服务端 Session。 */
@Service
public class IdentityAuthenticationService {

    /** Spring Security 认证入口。 */
    private final AuthenticationManager authenticationManager;

    /** 登录成功后的 Session 防护策略。 */
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    /** 将认证上下文持久化进 HttpSession。 */
    private final SecurityContextRepository securityContextRepository;

    /** 解析认证完成后的可信用户。 */
    private final CurrentUserProvider currentUserProvider;

    public IdentityAuthenticationService(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            CurrentUserProvider currentUserProvider) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /** 校验用户名密码，更换既有 Session ID，并保存新的认证上下文。 */
    public AuthenticatedUser login(
            String username,
            String password,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        UsernameNormalizer.normalize(username), password));
        // 登录成功后先执行 Session fixation 防护，再持久化可信认证上下文。
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return currentUserProvider.requireCurrentUser();
    }
}
