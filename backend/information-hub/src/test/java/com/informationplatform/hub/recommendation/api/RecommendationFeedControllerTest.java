package com.informationplatform.hub.recommendation.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.informationplatform.hub.recommendation.feed.application.RecommendationFeedRequestException;
import com.informationplatform.hub.recommendation.feed.application.RecommendationFeedService;
import com.informationplatform.hub.recommendation.feed.domain.RecommendationFeedView;
import java.math.BigDecimal;
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

/** 验证 Recommendation Feed V1 的 Session、默认分页、响应结构和稳定参数错误。 */
@WebMvcTest(RecommendationFeedController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class,
    ApiErrorLoggingResponseAdvice.class,
    IdentitySecurityConfiguration.class,
    RecommendationFeedApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=4096"
})
class RecommendationFeedControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RecommendationFeedService feedService;
    @MockitoBean private IdentityUserDetailsService identityUserDetailsService;

    @Test
    void rejectsAnonymousFeedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/JOB/feed"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @WithMockUser
    void returnsFeedContract() throws Exception {
        when(feedService.get("JOB", 2, 10)).thenReturn(feed());

        mockMvc.perform(get("/api/v1/recommendations/JOB/feed")
                        .param("page", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_FEED_FOUND"))
                .andExpect(jsonPath("$.data.run.id").value(301))
                .andExpect(jsonPath("$.data.run.profileChangedSinceRun").value(false))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(21))
                .andExpect(jsonPath("$.data.items[0].recommendationItemId").value(1001))
                .andExpect(jsonPath("$.data.items[0].scoreBreakdown.aiRelevanceScore").value(95.0))
                .andExpect(jsonPath("$.data.items[0].feedbackState").value("INTERESTED"))
                .andExpect(jsonPath("$.data.items[0].jobDisposition").value("CONTACTED"))
                .andExpect(jsonPath("$.data.items[0].viewed").value(true))
                .andExpect(jsonPath("$.data.items[0].job.title").value("Java Backend Engineer"))
                .andExpect(jsonPath("$.data.items[0].analysisId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].scoreBreakdownJson").doesNotExist());
    }

    @Test
    @WithMockUser
    void returnsStablePaginationError() throws Exception {
        when(feedService.get("JOB", 0, null)).thenThrow(
                new RecommendationFeedRequestException(
                        "RECOMMENDATION_FEED_PAGE_INVALID", "page must be at least 1"));

        mockMvc.perform(get("/api/v1/recommendations/JOB/feed").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_FEED_PAGE_INVALID"));
    }

    private RecommendationFeedView feed() {
        return new RecommendationFeedView(
                new RecommendationFeedView.Run(
                        301,
                        "JOB",
                        "ANALYSIS_BATCH_COMPLETED",
                        LocalDateTime.of(2026, 8, 9, 18, 0),
                        "JOB_RECOMMENDATION",
                        1,
                        false),
                2,
                10,
                21,
                List.of(new RecommendationFeedView.Item(
                        1001,
                        88,
                        102,
                        1,
                        new BigDecimal("92.400"),
                        new RecommendationFeedView.ScoreBreakdown(
                                new BigDecimal("95.000"),
                                new BigDecimal("86.000"),
                                new BigDecimal("90.000")),
                        List.of("AI 相关度高"),
                        "INTERESTED",
                        "CONTACTED",
                        true,
                        new RecommendationFeedView.Job(
                                "Java Backend Engineer",
                                "Example",
                                "20-35K",
                                "成都",
                                "REMOTE",
                                "https://example.test/job/88"))));
    }
}
