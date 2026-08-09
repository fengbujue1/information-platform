package com.informationplatform.hub.recommendation.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.informationplatform.hub.recommendation.run.application.RecommendationRunConflictException;
import com.informationplatform.hub.recommendation.run.application.RecommendationRunService;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunAccepted;
import com.informationplatform.hub.recommendation.run.domain.RecommendationRunView;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证 Recommendation Refresh/Run Contract 的 Session、CSRF、202、隔离查询和冲突。 */
@WebMvcTest(RecommendationRunController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class,
    ApiErrorLoggingResponseAdvice.class,
    IdentitySecurityConfiguration.class,
    RecommendationRunApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=4096"
})
class RecommendationRunControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RecommendationRunService runService;
    @MockitoBean private IdentityUserDetailsService identityUserDetailsService;

    @Test
    void rejectsAnonymousRunAccess() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/JOB/runs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @WithMockUser
    void requiresCsrfForManualRefresh() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations/JOB/refresh"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser
    void acceptsManualRefreshAndServesRunListAndDetail() throws Exception {
        RecommendationRunView view = view();
        when(runService.refresh("JOB"))
                .thenReturn(new RecommendationRunAccepted(301L, "PENDING"));
        when(runService.list("JOB", 20)).thenReturn(List.of(view));
        when(runService.get("JOB", 301L)).thenReturn(view);

        mockMvc.perform(post("/api/v1/recommendations/JOB/refresh").with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_RUN_ACCEPTED"))
                .andExpect(jsonPath("$.data.runId").value(301))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(get("/api/v1/recommendations/JOB/runs").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].informationType").value("JOB"))
                .andExpect(jsonPath("$.data[0].profileSnapshotJson").doesNotExist())
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());

        mockMvc.perform(get("/api/v1/recommendations/JOB/runs/301"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(301))
                .andExpect(jsonPath("$.data.algorithmKey").value("JOB_RECOMMENDATION"));
    }

    @Test
    @WithMockUser
    void returnsAcceptedConflictCode() throws Exception {
        when(runService.refresh("JOB"))
                .thenThrow(new RecommendationRunConflictException(
                        "RECOMMENDATION_RUN_IN_PROGRESS",
                        "A Manual Recommendation Run is already in progress"));

        mockMvc.perform(post("/api/v1/recommendations/JOB/refresh").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_RUN_IN_PROGRESS"));
    }

    private RecommendationRunView view() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 16, 0);
        return new RecommendationRunView(
                301,
                "JOB",
                "MANUAL",
                null,
                21,
                "a".repeat(64),
                31,
                32,
                "JOB_RECOMMENDATION",
                1,
                now.minusDays(7),
                now,
                10,
                8,
                5,
                "COMPLETED",
                null,
                null,
                null,
                now.minusMinutes(1),
                now,
                now.minusMinutes(2),
                now);
    }
}
