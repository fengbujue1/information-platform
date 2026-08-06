package com.informationplatform.hub.analysis.usage.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.informationplatform.hub.analysis.usage.application.AnalysisUsageService;
import com.informationplatform.hub.analysis.usage.domain.AnalysisUsagePeriod;
import com.informationplatform.hub.analysis.usage.domain.AnalysisUsageSummary;
import com.informationplatform.hub.common.api.ApiErrorWriter;
import com.informationplatform.hub.common.api.GlobalApiExceptionHandler;
import com.informationplatform.hub.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.informationplatform.hub.identity.infrastructure.security.IdentityUserDetailsService;
import com.informationplatform.hub.ingestion.api.CollectorApiProperties;
import com.informationplatform.hub.ingestion.api.CollectorRequestSizeFilter;
import com.informationplatform.hub.ingestion.api.CollectorTokenAuthenticationFilter;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalysisUsageController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    IdentitySecurityConfiguration.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=1024"
})
class AnalysisUsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisUsageService usageService;

    @MockitoBean
    private IdentityUserDetailsService identityUserDetailsService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/ai/usage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void returnsOwnerUsageWithoutCsrfForReadOnlyGet() throws Exception {
        when(usageService.getCurrentUserUsage()).thenReturn(summary());

        mockMvc.perform(get("/api/v1/ai/usage").with(user("owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ANALYSIS_USAGE_FOUND"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.data.today.actualTotalTokens").value(150))
                .andExpect(jsonPath("$.data.month.actualTotalTokens").value(700))
                .andExpect(jsonPath("$.data.allTime.actualTotalTokens").value(1300))
                .andExpect(jsonPath("$.data.allTime.periodStart").doesNotExist());
    }

    private AnalysisUsageSummary summary() {
        Instant now = Instant.parse("2026-08-05T16:30:00Z");
        return new AnalysisUsageSummary(
                "Asia/Shanghai",
                new AnalysisUsagePeriod(
                        Instant.parse("2026-08-05T16:00:00Z"),
                        now, 2, 1, 1, 100L, 50L, 150L),
                new AnalysisUsagePeriod(
                        Instant.parse("2026-07-31T16:00:00Z"),
                        now, 5, 4, 1, 500L, 200L, 700L),
                new AnalysisUsagePeriod(
                        null, null, 9, 7, 2, 900L, 400L, 1300L));
    }
}
