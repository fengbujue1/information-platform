package com.informationplatform.hub.recommendation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.informationplatform.hub.common.api.ApiErrorWriter;
import com.informationplatform.hub.common.api.GlobalApiExceptionHandler;
import com.informationplatform.hub.common.logging.ApiErrorLoggingResponseAdvice;
import com.informationplatform.hub.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.informationplatform.hub.identity.infrastructure.security.IdentityUserDetailsService;
import com.informationplatform.hub.ingestion.api.CollectorApiProperties;
import com.informationplatform.hub.ingestion.api.CollectorRequestSizeFilter;
import com.informationplatform.hub.ingestion.api.CollectorTokenAuthenticationFilter;
import com.informationplatform.hub.recommendation.application.RecommendationProfileChange;
import com.informationplatform.hub.recommendation.application.RecommendationProfileRequestException;
import com.informationplatform.hub.recommendation.application.RecommendationProfileService;
import com.informationplatform.hub.recommendation.domain.RecommendationProfileCore;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfile;
import com.informationplatform.hub.recommendation.job.domain.JobRecommendationProfilePreferences;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证 Recommendation Profile Contract 的 Session、CSRF、响应与 validation 日志。 */
@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(RecommendationProfileController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class,
    ApiErrorLoggingResponseAdvice.class,
    IdentitySecurityConfiguration.class,
    RecommendationProfileApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=4096"
})
class RecommendationProfileControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RecommendationProfileService profileService;

    @MockitoBean private IdentityUserDetailsService identityUserDetailsService;

    @Test
    void rejectsAnonymousRecommendationProfileAccess() throws Exception {
        mockMvc.perform(get("/api/v1/recommendation/profiles/JOB"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @WithMockUser
    void requiresCsrfForProfileWrites() throws Exception {
        mockMvc.perform(put("/api/v1/recommendation/profiles/JOB")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser
    void servesGetAndPutWithoutExposingUserId() throws Exception {
        JobRecommendationProfile profile = profile();
        when(profileService.getProfile("JOB")).thenReturn(profile);
        when(profileService.saveProfile(eq("JOB"), any()))
                .thenReturn(new RecommendationProfileChange(profile, true));

        mockMvc.perform(get("/api/v1/recommendation/profiles/JOB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_PROFILE_FOUND"))
                .andExpect(jsonPath("$.data.informationType").value("JOB"))
                .andExpect(jsonPath("$.data.userId").doesNotExist());

        mockMvc.perform(put("/api/v1/recommendation/profiles/JOB")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_PROFILE_CREATED"))
                .andExpect(jsonPath("$.data.targetRoles[0]").value("Java 后端"))
                .andExpect(jsonPath("$.data.contentHash").value("a".repeat(64)));
    }

    @Test
    @WithMockUser
    void returnsStableValidationErrorsAndWritesWarnLog(CapturedOutput output) throws Exception {
        mockMvc.perform(put("/api/v1/recommendation/profiles/JOB")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody().replace("\"windowDays\":7", "\"windowDays\":31")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        when(profileService.getProfile("EDUCATION"))
                .thenThrow(new RecommendationProfileRequestException(
                        "RECOMMENDATION_INFORMATION_TYPE_UNSUPPORTED",
                        "Only JOB Recommendation Profile is supported"));
        mockMvc.perform(get("/api/v1/recommendation/profiles/EDUCATION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("RECOMMENDATION_INFORMATION_TYPE_UNSUPPORTED"));

        org.assertj.core.api.Assertions.assertThat(output.getOut())
                .contains("Request rejected")
                .contains("RECOMMENDATION_INFORMATION_TYPE_UNSUPPORTED");
    }

    private JobRecommendationProfile profile() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 14, 0);
        return new JobRecommendationProfile(
                new RecommendationProfileCore(
                        21L, "JOB", 12L, 7, 50, "a".repeat(64), now, now),
                new JobRecommendationProfilePreferences(
                        List.of("Java 后端"),
                        List.of("Java", "Spring Boot"),
                        List.of("成都"),
                        List.of("HYBRID", "REMOTE"),
                        15_000,
                        List.of("纯销售")));
    }

    private String validBody() {
        return """
                {
                  "analysisPromptProfileId":12,
                  "windowDays":7,
                  "topN":50,
                  "targetRoles":["Java 后端"],
                  "preferredSkills":["Java","Spring Boot"],
                  "preferredCities":["成都"],
                  "preferredRemoteTypes":["REMOTE","HYBRID"],
                  "salaryMinMonthlyYuan":15000,
                  "excludedKeywords":["纯销售"]
                }
                """;
    }
}
