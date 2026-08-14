package com.informationplatform.hub.identity.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.informationplatform.hub.identity.application.IdentityBootstrapService;
import com.informationplatform.hub.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import com.informationplatform.hub.ingestion.api.dto.IngestionResult;
import com.informationplatform.hub.ingestion.application.InformationIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 验证 Identity Session/CSRF 与 Collector Bearer 区域保持隔离。 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.identity.bootstrap.username=",
    "information-hub.identity.bootstrap.password="
})
class IdentitySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerProperties serverProperties;

    @MockitoBean
    private UserAccountMapper userAccountMapper;

    @MockitoBean
    private IdentityBootstrapService bootstrapService;

    @MockitoBean
    private InformationIngestionService ingestionService;

    @BeforeEach
    void prepareAccount() {
        when(userAccountMapper.selectOne(any())).thenReturn(activeAccount());
    }

    @Test
    void loginRequiresCsrfAndCreatesAuthenticatedSession() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        CsrfSession csrf = fetchCsrf();
        String originalSessionId = csrf.session().getId();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOGIN_SUCCEEDED"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Shanghai"))
                .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession) loginResult.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotEquals(
                originalSessionId, authenticatedSession.getId());

        mockMvc.perform(get("/api/v1/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CURRENT_USER_FOUND"))
                .andExpect(jsonPath("$.data.displayName").value("Admin"));
    }

    @Test
    void rejectsBadCredentialsWithoutRevealingAccountState() throws Exception {
        CsrfSession csrf = fetchCsrf();

        mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"unknown","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void rejectsUnknownAccountWithTheSameAuthenticationFailure() throws Exception {
        when(userAccountMapper.selectOne(any())).thenReturn(null);
        CsrfSession csrf = fetchCsrf();

        mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void rejectsDisabledAccountWithTheSameAuthenticationFailure() throws Exception {
        UserAccountPo disabled = activeAccount();
        disabled.setStatus("DISABLED");
        when(userAccountMapper.selectOne(any())).thenReturn(disabled);
        CsrfSession csrf = fetchCsrf();

        mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void jobsAndAiRequireSessionWhileCollectorKeepsBearerAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/v1/ai/future"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/api/v1/collector/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COLLECTOR_AUTHENTICATION_FAILED"));

        when(ingestionService.ingest(any()))
                .thenReturn(new IngestionResult(1L, true, true, 1, true));
        mockMvc.perform(post("/api/v1/collector/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-collector-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCollectorBody()))
                .andExpect(status().isCreated());
    }

    @Test
    void httpLifecycleReturnsRequestIdAndLogsAuthenticatedUsername(CapturedOutput output)
            throws Exception {
        AuthenticatedSession authenticated = login();

        MvcResult result = mockMvc.perform(get("/api/v1/auth/me")
                        .session(authenticated.session()))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = result.getResponse().getHeader("X-Request-ID");
        assertThat(requestId).isNotBlank();
        assertThat(output).contains(
                "requestId=" + requestId,
                "username=admin",
                "HTTP request completed, method=GET, path=/api/v1/auth/me, status=200, durationMs=");
    }

    @Test
    void logoutRequiresCsrfAndInvalidatesSession() throws Exception {
        AuthenticatedSession authenticated = login();

        mockMvc.perform(post("/api/v1/auth/logout").session(authenticated.session()))
                .andExpect(status().isForbidden());

        CsrfSession csrf = fetchCsrf(authenticated.session());
        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOGOUT_SUCCEEDED"));

        mockMvc.perform(get("/api/v1/auth/me").session(csrf.session()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bindsSessionCookieSecurityAttributes() {
        org.springframework.boot.web.server.Cookie cookie =
                serverProperties.getServlet().getSession().getCookie();

        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, cookie.getHttpOnly());
        org.junit.jupiter.api.Assertions.assertEquals("/", cookie.getPath());
        org.junit.jupiter.api.Assertions.assertEquals(
                "LAX", cookie.getSameSite().name());
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, cookie.getSecure());
    }

    private AuthenticatedSession login() throws Exception {
        CsrfSession csrf = fetchCsrf();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isOk())
                .andReturn();
        return new AuthenticatedSession(
                (MockHttpSession) result.getRequest().getSession(false));
    }

    private CsrfSession fetchCsrf() throws Exception {
        return fetchCsrf(new MockHttpSession());
    }

    private CsrfSession fetchCsrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_CREATED"))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode data =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(content).get("data");
        return new CsrfSession(
                (MockHttpSession) result.getRequest().getSession(false),
                data.get("headerName").asText(),
                data.get("token").asText());
    }

    private UserAccountPo activeAccount() {
        UserAccountPo account = new UserAccountPo();
        account.setId(1L);
        account.setUsername("admin");
        account.setPasswordHash("{noop}secret");
        account.setDisplayName("Admin");
        account.setTimezone("Asia/Shanghai");
        account.setStatus("ACTIVE");
        return account;
    }

    private String loginBody() {
        return """
                {"username":" ADMIN ","password":"secret"}
                """;
    }

    private String validCollectorBody() {
        return """
                {
                  "schemaVersion":1,
                  "informationType":"JOB",
                  "source":"BOSS",
                  "sourceItemId":"identity-test",
                  "title":"Identity test",
                  "collectedAt":"2026-08-03T08:00:00Z",
                  "collector":{"collectorId":"test","collectorVersion":"1"},
                  "extension":{"remoteType":"UNKNOWN","jobStatus":"ACTIVE","detailStatus":"UNKNOWN"},
                  "rawPayload":{"list":{"encrypt_job_id":"identity-test"}}
                }
                """;
    }

    private record CsrfSession(
            /** CSRF token 关联的服务端 Session。 */
            MockHttpSession session,
            /** 客户端提交 token 使用的 Header。 */
            String headerName,
            /** 当前 Session 的 token。 */
            String token) {
    }

    private record AuthenticatedSession(
            /** 已登录的服务端 Session。 */
            MockHttpSession session) {
    }
}
