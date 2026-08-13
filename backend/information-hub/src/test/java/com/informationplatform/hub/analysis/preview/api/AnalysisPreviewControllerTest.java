package com.informationplatform.hub.analysis.preview.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewConfigurationException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewRequestException;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewService;
import com.informationplatform.hub.analysis.preview.domain.AnalysisPreview;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** 验证 Preview 的 Session、CSRF、安全响应和稳定错误。 */
@WebMvcTest(AnalysisPreviewController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class,
    IdentitySecurityConfiguration.class,
    AnalysisPreviewApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=1024"
})
class AnalysisPreviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisPreviewService previewService;

    @MockitoBean
    private IdentityUserDetailsService identityUserDetailsService;

    @Test
    void rejectsAnonymousPreview() throws Exception {
        mockMvc.perform(post("/api/v1/ai/analysis-batches/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @WithMockUser
    void requiresCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/ai/analysis-batches/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void returnsServiceUnavailableWithoutSigningConfiguration() throws Exception {
        when(previewService.preview(11, 3, 20, 75_000L))
                .thenThrow(new AnalysisPreviewConfigurationException("secret is unavailable"));

        mockMvc.perform(post("/api/v1/ai/analysis-batches/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ANALYSIS_PREVIEW_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.message").value("分析预览服务尚未配置"));
    }

    @Test
    @WithMockUser
    void returnsOwnerSafePreviewAndStableLimitError() throws Exception {
        when(previewService.preview(11, 3, 20, 75_000L)).thenReturn(preview());

        mockMvc.perform(post("/api/v1/ai/analysis-batches/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ANALYSIS_PREVIEW_CREATED"))
                .andExpect(jsonPath("$.data.totalInWindow").value(5))
                .andExpect(jsonPath("$.data.eligibleCount").value(4))
                .andExpect(jsonPath("$.data.pendingCount").value(4))
                .andExpect(jsonPath("$.data.selectedCount").value(3))
                .andExpect(jsonPath("$.data.estimateMethod")
                        .value("UTF8_BYTES_DIV3_MARGIN20_V1"))
                .andExpect(jsonPath("$.data.previewToken").value("signed-token"))
                .andExpect(jsonPath("$.data.userId").doesNotExist());

        when(previewService.preview(11, 15, 20, 75_000L))
                .thenThrow(new AnalysisPreviewRequestException(
                        "PREVIEW_WINDOW_DAYS_INVALID", "windowDays is invalid"));
        mockMvc.perform(post("/api/v1/ai/analysis-batches/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody().replace("\"windowDays\":3", "\"windowDays\":15")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PREVIEW_WINDOW_DAYS_INVALID"));
    }

    private String requestBody() {
        return """
                {
                  "promptProfileId":11,
                  "windowDays":3,
                  "maxCandidates":20,
                  "maxEstimatedTokens":75000
                }
                """;
    }

    private AnalysisPreview preview() {
        Instant end = Instant.parse("2026-08-05T06:00:00Z");
        return new AnalysisPreview(
                end.minusSeconds(259_200), end, 5, 4, 4, 1, 3, 0, 1,
                300, 3_000, 3_300, "UTF8_BYTES_DIV3_MARGIN20_V1",
                end.plusSeconds(600), "signed-token");
    }
}
