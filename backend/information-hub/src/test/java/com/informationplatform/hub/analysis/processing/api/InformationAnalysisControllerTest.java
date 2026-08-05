package com.informationplatform.hub.analysis.processing.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.processing.application.AnalysisNotFoundException;
import com.informationplatform.hub.analysis.processing.application.InformationAnalysisService;
import com.informationplatform.hub.analysis.processing.domain.InformationAnalysisView;
import com.informationplatform.hub.analysis.processing.domain.ModelInvocationView;
import com.informationplatform.hub.common.api.ApiErrorWriter;
import com.informationplatform.hub.common.api.GlobalApiExceptionHandler;
import com.informationplatform.hub.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.informationplatform.hub.identity.infrastructure.security.IdentityUserDetailsService;
import com.informationplatform.hub.ingestion.api.CollectorApiProperties;
import com.informationplatform.hub.ingestion.api.CollectorRequestSizeFilter;
import com.informationplatform.hub.ingestion.api.CollectorTokenAuthenticationFilter;
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

/** 验证单条 Analysis 的 Session、CSRF、Owner 安全响应和错误边界。 */
@WebMvcTest(InformationAnalysisController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class,
    IdentitySecurityConfiguration.class,
    InformationAnalysisApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=1024"
})
class InformationAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InformationAnalysisService analysisService;

    @MockitoBean
    private IdentityUserDetailsService identityUserDetailsService;

    @Test
    void rejectsAnonymousAnalysisAccess() throws Exception {
        mockMvc.perform(get("/api/v1/ai/analyses/31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @WithMockUser
    void requiresCsrfForExecution() throws Exception {
        mockMvc.perform(post("/api/v1/ai/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void executesAndQueriesOwnerSafeAnalysis() throws Exception {
        InformationAnalysisView view = view();
        when(analysisService.execute(11, 12L, 21, false)).thenReturn(view);
        when(analysisService.get(31)).thenReturn(view);

        mockMvc.perform(post("/api/v1/ai/analyses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("INFORMATION_ANALYSIS_RESOLVED"))
                .andExpect(jsonPath("$.data.snapshotId").value(12))
                .andExpect(jsonPath("$.data.promptVersionId").value(22))
                .andExpect(jsonPath("$.data.resultJson.relevanceScore").value(82))
                .andExpect(jsonPath("$.data.invocations[0].totalTokens").value(12))
                .andExpect(jsonPath("$.data.userId").doesNotExist());

        mockMvc.perform(get("/api/v1/ai/analyses/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("INFORMATION_ANALYSIS_FOUND"))
                .andExpect(jsonPath("$.data.id").value(31));
    }

    @Test
    @WithMockUser
    void hidesCrossOwnerAnalysisAsNotFound() throws Exception {
        when(analysisService.get(99)).thenThrow(new AnalysisNotFoundException(
                "ANALYSIS_NOT_FOUND", "Analysis does not exist"));

        mockMvc.perform(get("/api/v1/ai/analyses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_NOT_FOUND"));
    }

    private String requestBody() {
        return """
                {
                  "informationId":11,
                  "snapshotId":12,
                  "promptProfileId":21
                }
                """;
    }

    private InformationAnalysisView view() {
        LocalDateTime time = LocalDateTime.of(2026, 8, 5, 10, 0);
        ObjectMapper mapper = new ObjectMapper();
        var result = mapper.createObjectNode()
                .put("schemaVersion", 1)
                .put("relevanceScore", 82)
                .put("summary", "相关");
        ModelInvocationView invocation = new ModelInvocationView(
                41, 1, "FAKE", "fake-model", "request-1", "SUCCEEDED", "stop",
                10L, 2L, 12L, null, null, "REPORTED", 3L,
                null, null, time, time);
        return new InformationAnalysisView(
                31, 11, 12, "JOB", "JOB_USER_RELEVANCE", 1,
                "USER_RELEVANCE", 21, 22, "SUCCEEDED", result, 82, "相关",
                100L, 1000L, 1100L, "UTF8_BYTES_DIV3_MARGIN20_V1",
                null, null, time, time, time, time, List.of(invocation));
    }
}
