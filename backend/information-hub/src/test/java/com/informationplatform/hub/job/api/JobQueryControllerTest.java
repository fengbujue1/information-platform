package com.informationplatform.hub.job.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.informationplatform.hub.common.api.ApiErrorWriter;
import com.informationplatform.hub.common.api.GlobalApiExceptionHandler;
import com.informationplatform.hub.ingestion.api.CollectorApiProperties;
import com.informationplatform.hub.ingestion.api.CollectorRequestSizeFilter;
import com.informationplatform.hub.ingestion.api.CollectorTokenAuthenticationFilter;
import com.informationplatform.hub.job.api.dto.JobDetailResponse;
import com.informationplatform.hub.job.api.dto.JobListItemResponse;
import com.informationplatform.hub.job.api.dto.JobSnapshotResponse;
import com.informationplatform.hub.job.api.dto.PageResponse;
import com.informationplatform.hub.job.application.JobNotFoundException;
import com.informationplatform.hub.job.application.JobQueryRequestException;
import com.informationplatform.hub.job.application.JobQueryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobQueryController.class)
@Import({
    CollectorApiProperties.class,
    CollectorTokenAuthenticationFilter.class,
    CollectorRequestSizeFilter.class,
    ApiErrorWriter.class,
    GlobalApiExceptionHandler.class
})
@TestPropertySource(properties = {
    "information-hub.collector-api.token=test-collector-token",
    "information-hub.collector-api.max-request-bytes=1024"
})
class JobQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobQueryService queryService;

    @Test
    void returnsPagedJobsWithoutContentOrRawPayload() throws Exception {
        when(queryService.listJobs(any())).thenReturn(new PageResponse<>(
                2,
                10,
                11,
                2,
                List.of(listItem())));

        mockMvc.perform(get("/api/v1/jobs")
                        .queryParam("page", "2")
                        .queryParam("size", "10")
                        .queryParam("city", "成都"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("JOBS_FOUND"))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.items[0].id").value(7))
                .andExpect(jsonPath("$.data.items[0].content").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].rawPayload").doesNotExist());
    }

    @Test
    void returnsDetailWithoutRawPayload() throws Exception {
        when(queryService.getJob(7L)).thenReturn(detail());

        mockMvc.perform(get("/api/v1/jobs/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("JOB_FOUND"))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.content").value("Job description"))
                .andExpect(jsonPath("$.data.rawPayload").doesNotExist());
    }

    @Test
    void returnsSnapshotsWithoutRawPayload() throws Exception {
        when(queryService.getSnapshots(7L)).thenReturn(List.of(snapshot()));

        mockMvc.perform(get("/api/v1/jobs/7/snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("JOB_SNAPSHOTS_FOUND"))
                .andExpect(jsonPath("$.data[0].versionNo").value(2))
                .andExpect(jsonPath("$.data[0].rawPayload").doesNotExist());
    }

    @Test
    void returnsStableErrorsForInvalidQueryAndMissingJob() throws Exception {
        when(queryService.listJobs(any())).thenThrow(new JobQueryRequestException(
                "INVALID_JOB_PAGE_SIZE", "size must be between 1 and 100"));
        when(queryService.getJob(404L)).thenThrow(new JobNotFoundException(404L));

        mockMvc.perform(get("/api/v1/jobs").queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JOB_PAGE_SIZE"));
        mockMvc.perform(get("/api/v1/jobs/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));
    }

    @Test
    void rejectsNonNumericJobIdAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));
    }

    private JobListItemResponse listItem() {
        return new JobListItemResponse(
                7,
                "BOSS",
                "source-7",
                "https://example.test/jobs/7",
                "Java developer",
                "Example company",
                "20-30K",
                20000,
                30000,
                12,
                "成都·武侯区",
                "成都",
                "3-5年",
                "本科",
                "REMOTE",
                "ACTIVE",
                null,
                Instant.parse("2026-07-28T01:00:00Z"),
                Instant.parse("2026-07-28T02:00:00Z"),
                2);
    }

    private JobDetailResponse detail() {
        return new JobDetailResponse(
                7,
                "BOSS",
                "source-7",
                "https://example.test/jobs/7",
                "Java developer",
                "Job description",
                null,
                Instant.parse("2026-07-28T01:00:00Z"),
                Instant.parse("2026-07-28T01:00:00Z"),
                Instant.parse("2026-07-28T02:00:00Z"),
                2,
                "collector",
                "1.0",
                "company-1",
                "recruiter-1",
                "Example company",
                null,
                null,
                null,
                null,
                "20-30K",
                "SOURCE",
                20000,
                30000,
                12,
                "成都·武侯区",
                "成都",
                "武侯区",
                null,
                "3-5年",
                "本科",
                null,
                "HR",
                null,
                "REMOTE",
                "ACTIVE",
                "FETCHED",
                null,
                null,
                null,
                null);
    }

    private JobSnapshotResponse snapshot() {
        return new JobSnapshotResponse(
                2,
                2,
                "a".repeat(64),
                "Java developer",
                "Job description",
                null,
                Instant.parse("2026-07-28T01:00:00Z"),
                "collector",
                "1.0",
                Instant.parse("2026-07-28T02:00:00Z"));
    }
}
