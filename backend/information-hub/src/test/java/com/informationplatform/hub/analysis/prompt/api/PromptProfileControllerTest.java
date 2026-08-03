package com.informationplatform.hub.analysis.prompt.api;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.informationplatform.hub.analysis.prompt.application.PromptNotFoundException;
import com.informationplatform.hub.analysis.prompt.application.PromptProfileService;
import com.informationplatform.hub.analysis.prompt.domain.PromptProfile;
import com.informationplatform.hub.analysis.prompt.domain.PromptProfileStatus;
import com.informationplatform.hub.analysis.prompt.domain.PromptVersion;
import com.informationplatform.hub.analysis.prompt.domain.PromptVersionChange;
import com.informationplatform.hub.common.api.ApiErrorWriter;
import com.informationplatform.hub.common.api.GlobalApiExceptionHandler;
import com.informationplatform.hub.ingestion.api.CollectorApiProperties;
import com.informationplatform.hub.ingestion.api.CollectorRequestSizeFilter;
import com.informationplatform.hub.ingestion.api.CollectorTokenAuthenticationFilter;
import com.informationplatform.hub.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.informationplatform.hub.identity.infrastructure.security.IdentityUserDetailsService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** 验证 AI Prompt Contract 的 Session、CSRF、响应与错误边界。 */
@WebMvcTest(PromptProfileController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class,
    IdentitySecurityConfiguration.class,
    PromptApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=1024"
})
class PromptProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromptProfileService promptProfileService;

    @MockitoBean
    private IdentityUserDetailsService identityUserDetailsService;

    @Test
    void rejectsAnonymousPromptAccess() throws Exception {
        mockMvc.perform(get("/api/v1/ai/prompt-profiles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @WithMockUser
    void servesAllReadContractEndpoints() throws Exception {
        when(promptProfileService.listProfiles()).thenReturn(List.of(profile(10L, 20L)));
        when(promptProfileService.getProfile(10L)).thenReturn(profile(10L, 20L));
        when(promptProfileService.listVersions(10L)).thenReturn(List.of(version(20L, 10L)));

        mockMvc.perform(get("/api/v1/ai/prompt-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROMPT_PROFILES_FOUND"))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());
        mockMvc.perform(get("/api/v1/ai/prompt-profiles/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROMPT_PROFILE_FOUND"))
                .andExpect(jsonPath("$.data.activeVersionId").value(20));
        mockMvc.perform(get("/api/v1/ai/prompt-profiles/10/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROMPT_VERSIONS_FOUND"))
                .andExpect(jsonPath("$.data[0].contentHash").value("a".repeat(64)));
    }

    @Test
    @WithMockUser
    void requiresCsrfForPromptWrites() throws Exception {
        mockMvc.perform(post("/api/v1/ai/prompt-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProfileBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void servesAllWriteContractEndpointsWithCsrf() throws Exception {
        when(promptProfileService.createProfile(anyString(), anyString()))
                .thenReturn(profile(10L, null));
        when(promptProfileService.createVersion(anyLong(), anyString()))
                .thenReturn(new PromptVersionChange(version(20L, 10L), true));
        when(promptProfileService.activateVersion(10L, 20L))
                .thenReturn(profile(10L, 20L));
        when(promptProfileService.updateStatus(10L, "DISABLED"))
                .thenReturn(new PromptProfile(
                        10L,
                        "Default",
                        "JOB_USER_RELEVANCE",
                        20L,
                        PromptProfileStatus.DISABLED,
                        timestamp(),
                        timestamp()));

        mockMvc.perform(post("/api/v1/ai/prompt-profiles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProfileBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROMPT_PROFILE_CREATED"));
        mockMvc.perform(post("/api/v1/ai/prompt-profiles/10/versions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"关注 Java 后端岗位"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROMPT_VERSION_CREATED"));
        mockMvc.perform(put("/api/v1/ai/prompt-profiles/10/active-version")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"versionId":20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROMPT_ACTIVE_VERSION_UPDATED"));
        mockMvc.perform(put("/api/v1/ai/prompt-profiles/10/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    @Test
    @WithMockUser
    void hidesCrossOwnerProfileAsNotFound() throws Exception {
        when(promptProfileService.getProfile(99L))
                .thenThrow(new PromptNotFoundException(
                        "PROMPT_PROFILE_NOT_FOUND", "Prompt Profile does not exist"));

        mockMvc.perform(get("/api/v1/ai/prompt-profiles/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMPT_PROFILE_NOT_FOUND"));
    }

    private PromptProfile profile(long id, Long activeVersionId) {
        return new PromptProfile(
                id,
                "Default",
                "JOB_USER_RELEVANCE",
                activeVersionId,
                PromptProfileStatus.ACTIVE,
                timestamp(),
                timestamp());
    }

    private PromptVersion version(long id, long profileId) {
        return new PromptVersion(
                id,
                profileId,
                1,
                "关注 Java 后端岗位",
                "a".repeat(64),
                timestamp());
    }

    private LocalDateTime timestamp() {
        return LocalDateTime.of(2026, 8, 3, 10, 0);
    }

    private String createProfileBody() {
        return """
                {
                  "name":"Default",
                  "analysisDefinitionKey":"JOB_USER_RELEVANCE"
                }
                """;
    }
}
