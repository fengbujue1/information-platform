package com.informationplatform.hub.analysis.batch.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.informationplatform.hub.analysis.batch.application.AnalysisBatchService;
import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchProgress;
import com.informationplatform.hub.analysis.batch.domain.AnalysisBatchView;
import com.informationplatform.hub.common.api.ApiErrorWriter;
import com.informationplatform.hub.common.api.GlobalApiExceptionHandler;
import com.informationplatform.hub.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.informationplatform.hub.identity.infrastructure.security.IdentityUserDetailsService;
import com.informationplatform.hub.ingestion.api.CollectorApiProperties;
import com.informationplatform.hub.ingestion.api.CollectorRequestSizeFilter;
import com.informationplatform.hub.ingestion.api.CollectorTokenAuthenticationFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalysisBatchController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    AnalysisBatchApiExceptionHandler.class,
    IdentitySecurityConfiguration.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=1024"
})
class AnalysisBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisBatchService batchService;

    @MockitoBean
    private IdentityUserDetailsService identityUserDetailsService;

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/ai/analysis-batches/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"previewToken\":\"token\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/ai/analysis-batches/confirm")
                        .with(user("owner"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"previewToken\":\"token\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsFrozenBatchAsynchronously() throws Exception {
        when(batchService.confirm("token")).thenReturn(view());

        mockMvc.perform(post("/api/v1/ai/analysis-batches/confirm")
                        .with(user("owner"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"previewToken\":\"token\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("ANALYSIS_BATCH_ACCEPTED"))
                .andExpect(jsonPath("$.data.id").value(31))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    private AnalysisBatchView view() {
        return new AnalysisBatchView(
                31, "MANUAL", 11, 12, "JOB", "JOB_USER_RELEVANCE", 1,
                "FIRST_INGESTED", 3,
                null, null, 20, 75_000, 1, 1, 0, 1, 0, 0,
                100L, 1000L, 1100L, "UTF8_BYTES_DIV3_MARGIN20_V1",
                "PENDING", null, null, null, null, null,
                new AnalysisBatchProgress(1, 1, 0, 0, 0, 0, 0, 0, 0,
                        null, null, null),
                List.of());
    }
}
