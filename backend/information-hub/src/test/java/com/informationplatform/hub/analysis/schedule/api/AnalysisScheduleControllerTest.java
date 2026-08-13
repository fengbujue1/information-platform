package com.informationplatform.hub.analysis.schedule.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.informationplatform.hub.analysis.schedule.application.AnalysisScheduleService;
import com.informationplatform.hub.analysis.schedule.application.AnalysisScheduleRequestException;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleCommand;
import com.informationplatform.hub.analysis.schedule.domain.AnalysisScheduleView;
import com.informationplatform.hub.common.api.ApiErrorWriter;
import com.informationplatform.hub.common.api.GlobalApiExceptionHandler;
import com.informationplatform.hub.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.informationplatform.hub.identity.infrastructure.security.IdentityUserDetailsService;
import com.informationplatform.hub.ingestion.api.CollectorApiProperties;
import com.informationplatform.hub.ingestion.api.CollectorRequestSizeFilter;
import com.informationplatform.hub.ingestion.api.CollectorTokenAuthenticationFilter;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 验证 Schedule API 的 Session、CSRF、Owner 响应和路由 Contract。 */
@WebMvcTest(AnalysisScheduleController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class,
    IdentitySecurityConfiguration.class,
    AnalysisScheduleApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=1024"
})
class AnalysisScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisScheduleService scheduleService;

    @MockitoBean
    private IdentityUserDetailsService identityUserDetailsService;

    @Test
    void rejectsAnonymousScheduleAccess() throws Exception {
        mockMvc.perform(get("/api/v1/ai/analysis-schedules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @WithMockUser
    void requiresCsrfForScheduleWrites() throws Exception {
        mockMvc.perform(post("/api/v1/ai/analysis-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void servesScheduleCrudAndStatusRoutes() throws Exception {
        AnalysisScheduleView view = view();
        when(scheduleService.list()).thenReturn(List.of(view));
        when(scheduleService.get(31)).thenReturn(view);
        when(scheduleService.create(
                        any(AnalysisScheduleCommand.class), nullable(Boolean.class)))
                .thenReturn(view);
        when(scheduleService.update(anyLong(), any(AnalysisScheduleCommand.class)))
                .thenReturn(view);
        when(scheduleService.updateStatus(anyLong(), anyBoolean())).thenReturn(view);

        mockMvc.perform(get("/api/v1/ai/analysis-schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(31))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());
        mockMvc.perform(get("/api/v1/ai/analysis-schedules/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ANALYSIS_SCHEDULE_FOUND"));
        mockMvc.perform(post("/api/v1/ai/analysis-schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ANALYSIS_SCHEDULE_CREATED"));
        mockMvc.perform(put("/api/v1/ai/analysis-schedules/31")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ANALYSIS_SCHEDULE_UPDATED"));
        mockMvc.perform(put("/api/v1/ai/analysis-schedules/31/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ANALYSIS_SCHEDULE_STATUS_UPDATED"));
    }

    @Test
    @WithMockUser
    void returnsChinesePublicMessageForDisabledPromptProfile() throws Exception {
        when(scheduleService.create(any(AnalysisScheduleCommand.class), nullable(Boolean.class)))
                .thenThrow(new AnalysisScheduleRequestException(
                        "ANALYSIS_SCHEDULE_PROFILE_DISABLED",
                        "Prompt Profile must be active"));

        mockMvc.perform(post("/api/v1/ai/analysis-schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ANALYSIS_SCHEDULE_PROFILE_DISABLED"))
                .andExpect(jsonPath("$.message").value(
                        "所选提示词方案已停用，请先启用后再保存定时分析"));
    }
    private AnalysisScheduleView view() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 5, 12, 0);
        return new AnalysisScheduleView(
                31,
                "每日职位",
                11,
                false,
                LocalTime.of(2, 0),
                "Asia/Shanghai",
                3,
                20,
                75_000,
                null,
                timestamp,
                timestamp,
                null);
    }

    private String body() {
        return """
                {
                  "name":"每日职位",
                  "promptProfileId":11,
                  "localTime":"02:00:00",
                  "timezone":"Asia/Shanghai",
                  "windowDays":3,
                  "maxCandidates":20,
                  "maxEstimatedTokens":75000
                }
                """;
    }
}
