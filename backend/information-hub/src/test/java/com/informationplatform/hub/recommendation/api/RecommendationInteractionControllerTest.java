package com.informationplatform.hub.recommendation.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.informationplatform.hub.recommendation.application.RecommendationInteractionRequestException;
import com.informationplatform.hub.recommendation.application.RecommendationInteractionService;
import com.informationplatform.hub.recommendation.domain.FeedbackState;
import com.informationplatform.hub.recommendation.domain.UserInformationInteraction;
import com.informationplatform.hub.recommendation.job.domain.JobDisposition;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证 Interaction Contract 的 Session、CSRF、请求映射与 Owner 隐藏。 */
@WebMvcTest(RecommendationInteractionController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class,
    ApiErrorLoggingResponseAdvice.class,
    IdentitySecurityConfiguration.class,
    RecommendationInteractionApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=4096"
})
class RecommendationInteractionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RecommendationInteractionService interactionService;

    @MockitoBean private IdentityUserDetailsService identityUserDetailsService;

    @Test
    void rejectsAnonymousInteractionWrites() throws Exception {
        mockMvc.perform(post("/api/v1/recommendation/interactions/7/view").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @WithMockUser
    void requiresCsrfForEveryInteractionWrite() throws Exception {
        mockMvc.perform(post("/api/v1/recommendation/interactions/7/view"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/recommendation/interactions/7/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackState\":\"INTERESTED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/recommendation/interactions/7/job-disposition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobDisposition\":\"CONTACTED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void mapsViewFeedbackAndDispositionWithoutExposingUserId() throws Exception {
        when(interactionService.recordView(7L, null)).thenReturn(state(1, FeedbackState.NONE,
                JobDisposition.NONE));
        when(interactionService.replaceFeedback(7L, "INTERESTED", 101L))
                .thenReturn(state(1, FeedbackState.INTERESTED, JobDisposition.NONE));
        when(interactionService.replaceJobDisposition(7L, "CONTACTED", 101L))
                .thenReturn(state(1, FeedbackState.INTERESTED, JobDisposition.CONTACTED));

        mockMvc.perform(post("/api/v1/recommendation/interactions/7/view").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("RECOMMENDATION_INTERACTION_VIEW_RECORDED"))
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.data.userId").doesNotExist());

        mockMvc.perform(put("/api/v1/recommendation/interactions/7/feedback")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feedbackState":"INTERESTED","recommendationItemId":101}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackState").value("INTERESTED"))
                .andExpect(jsonPath("$.data.jobDisposition").value("NONE"));

        mockMvc.perform(put("/api/v1/recommendation/interactions/7/job-disposition")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jobDisposition":"CONTACTED","recommendationItemId":101}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackState").value("INTERESTED"))
                .andExpect(jsonPath("$.data.jobDisposition").value("CONTACTED"));
    }

    @Test
    @WithMockUser
    void returnsStableValidationAndBusinessErrors() throws Exception {
        mockMvc.perform(put("/api/v1/recommendation/interactions/7/feedback")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackState\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        when(interactionService.recordView(7L, null))
                .thenThrow(new RecommendationInteractionRequestException(
                        "RECOMMENDATION_INTERACTION_ATTRIBUTION_INVALID",
                        "recommendationItemId is not a valid attribution"));
        mockMvc.perform(post("/api/v1/recommendation/interactions/7/view").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("RECOMMENDATION_INTERACTION_ATTRIBUTION_INVALID"));
    }

    private UserInformationInteraction state(
            int viewCount, FeedbackState feedbackState, JobDisposition disposition) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 16, 0);
        return new UserInformationInteraction(
                7L,
                viewCount,
                viewCount == 0 ? null : now,
                feedbackState,
                feedbackState == FeedbackState.NONE ? null : now,
                disposition,
                disposition == JobDisposition.NONE ? null : now,
                disposition == JobDisposition.NONE && feedbackState == FeedbackState.NONE
                        ? null
                        : 101L,
                now);
    }
}
