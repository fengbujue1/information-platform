package com.informationplatform.hub.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.common.api.ApiErrorWriter;
import com.informationplatform.hub.common.api.ApiResponse;
import com.informationplatform.hub.common.logging.AuthenticatedUserMdcFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/** 定义浏览器 Session 区域与 Collector Bearer 区域的安全边界。 */
@Configuration
@EnableWebSecurity
public class IdentitySecurityConfiguration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IdentitySecurityConfiguration.class);

    /** 配置 API 授权、CSRF、Logout、Session 和统一 JSON 安全错误。 */
    @Bean
    SecurityFilterChain identitySecurityFilterChain(
            HttpSecurity http,
            ApiErrorWriter errorWriter,
            ObjectMapper objectMapper,
            SecurityContextRepository securityContextRepository)
            throws Exception {
        HttpSessionCsrfTokenRepository csrfTokenRepository =
                new HttpSessionCsrfTokenRepository();

        http.securityContext(context ->
                        context.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/login")
                        .permitAll()
                        // Collector 继续由既有恒定时间 Bearer Filter 独立认证。
                        .requestMatchers("/api/v1/collector/**")
                        .permitAll()
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/jobs/**",
                                "/api/v1/ai/**",
                                "/api/v1/recommendation/**",
                                "/api/v1/recommendations/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers("/api/v1/collector/**"))
                .sessionManagement(session ->
                        session.sessionFixation(fixation -> fixation.changeSessionId()))
                .requestCache(cache -> cache.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                // SecurityContext 加载后只把可信认证用户名写入本次请求的 MDC。
                .addFilterAfter(new AuthenticatedUserMdcFilter(), SecurityContextHolderFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler((request, response, authentication) ->
                                writeSuccess(
                                        response,
                                        objectMapper,
                                        "LOGOUT_SUCCEEDED",
                                        java.util.Map.of("loggedOut", true))))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            LOGGER.warn(
                                    "Security request rejected, path={}, errorCode={}, reason={}",
                                    request.getRequestURI(),
                                    "AUTHENTICATION_REQUIRED",
                                    "authenticated_session_required");
                            errorWriter.write(
                                    response,
                                    401,
                                    "AUTHENTICATION_REQUIRED",
                                    "登录状态已失效，请重新登录");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            LOGGER.warn(
                                    "Security request rejected, path={}, errorCode={}, reason={}",
                                    request.getRequestURI(),
                                    "ACCESS_DENIED",
                                    "authorization_or_csrf_rejected");
                            errorWriter.write(
                                    response,
                                    403,
                                    "ACCESS_DENIED",
                                    "没有权限执行此操作");
                        }));
        return http.build();
    }

    /** 使用 Spring Security 自适应委托编码器，当前新密码默认采用 bcrypt。 */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** 使用 user_account 数据源和统一密码编码器构造认证管理器。 */
    @Bean
    AuthenticationManager authenticationManager(
            IdentityUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /** 登录后变更既有 Session ID，防止 Session fixation。 */
    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    /** 显式保存 Controller 登录建立的 SecurityContext。 */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** Logout 在过滤器阶段返回项目统一成功包络。 */
    private void writeSuccess(
            jakarta.servlet.http.HttpServletResponse response,
            ObjectMapper objectMapper,
            String code,
            Object data)
            throws IOException {
        response.setStatus(200);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.success(code, data));
    }
}
