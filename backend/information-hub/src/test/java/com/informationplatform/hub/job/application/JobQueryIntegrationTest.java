package com.informationplatform.hub.job.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.ingestion.api.dto.CollectorRequest;
import com.informationplatform.hub.ingestion.api.dto.InformationEnvelopeRequest;
import com.informationplatform.hub.ingestion.api.dto.IngestionResult;
import com.informationplatform.hub.ingestion.api.dto.JobExtensionRequest;
import com.informationplatform.hub.ingestion.application.InformationIngestionService;
import com.informationplatform.hub.job.api.dto.JobDetailResponse;
import com.informationplatform.hub.job.api.dto.JobListItemResponse;
import com.informationplatform.hub.job.api.dto.JobQueryRequest;
import com.informationplatform.hub.job.api.dto.JobSnapshotResponse;
import com.informationplatform.hub.job.api.dto.PageResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class JobQueryIntegrationTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> required("INFORMATION_HUB_TEST_DB_URL"));
        registry.add(
                "spring.datasource.username",
                () -> required("INFORMATION_HUB_TEST_DB_USERNAME"));
        registry.add(
                "spring.datasource.password",
                () -> required("INFORMATION_HUB_TEST_DB_PASSWORD"));
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private InformationIngestionService ingestionService;

    @Autowired
    private JobQueryService queryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    void filtersSalaryRangeAndPaginatesWithWhitelistedSort() {
        String marker = "query-" + UUID.randomUUID();
        ingest(request(
                marker + "-a",
                marker + " Java",
                "Spring service",
                "Alpha " + marker,
                "成都",
                20000,
                30000,
                "HYBRID",
                "ACTIVE"));
        ingest(request(
                marker + "-b",
                marker + " Data",
                "Python service",
                "Beta " + marker,
                "上海",
                30000,
                40000,
                "REMOTE",
                "OFFLINE"));
        ingest(request(
                marker + "-c",
                marker + " Platform",
                "Go service",
                "Gamma " + marker,
                "北京",
                40000,
                50000,
                "ONSITE",
                "ACTIVE"));

        PageResponse<JobListItemResponse> secondPage = queryService.listJobs(new JobQueryRequest(
                2,
                2,
                marker,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "salaryMinMonthlyYuan",
                "asc"));
        PageResponse<JobListItemResponse> filtered = queryService.listJobs(new JobQueryRequest(
                1,
                20,
                "Python",
                "Beta",
                "上海",
                35000,
                45000,
                "BOSS",
                "OFFLINE",
                "REMOTE",
                "firstSeenTime",
                "desc"));

        assertEquals(3, secondPage.total());
        assertEquals(2, secondPage.totalPages());
        assertEquals(40000, secondPage.items().getFirst().salaryMinMonthlyYuan());
        assertEquals(1, filtered.total());
        assertEquals(marker + "-b", filtered.items().getFirst().sourceItemId());
    }

    @Test
    @Transactional
    void returnsCurrentDetailAndSnapshotsNewestFirst() {
        String sourceItemId = "query-version-" + UUID.randomUUID();
        IngestionResult initial = ingest(request(
                sourceItemId,
                "Versioned job",
                "版本 A",
                "Version company",
                "成都",
                20000,
                30000,
                "UNKNOWN",
                "ACTIVE"));
        ingest(request(
                sourceItemId,
                "Versioned job",
                "版本 B",
                "Version company",
                "成都",
                25000,
                35000,
                "UNKNOWN",
                "ACTIVE"));

        JobDetailResponse detail = queryService.getJob(initial.informationId());
        List<JobSnapshotResponse> snapshots =
                queryService.getSnapshots(initial.informationId());

        assertEquals("版本 B", detail.content());
        assertEquals(2, detail.currentVersionNo());
        assertEquals(List.of(2, 1), snapshots.stream()
                .map(JobSnapshotResponse::versionNo)
                .toList());
        assertFalse(snapshots.getFirst().standardizedPayload().has("rawPayload"));
    }

    private IngestionResult ingest(InformationEnvelopeRequest request) {
        return ingestionService.ingest(request);
    }

    private InformationEnvelopeRequest request(
            String sourceItemId,
            String title,
            String content,
            String company,
            String city,
            int salaryMinimum,
            int salaryMaximum,
            String remoteType,
            String jobStatus) {
        try {
            return new InformationEnvelopeRequest(
                    1,
                    "JOB",
                    "BOSS",
                    sourceItemId,
                    "https://example.test/jobs/" + sourceItemId,
                    title,
                    content,
                    null,
                    OffsetDateTime.parse("2026-07-28T08:00:00Z"),
                    new CollectorRequest("query-integration-test", "1.0"),
                    objectMapper.readTree("{\"runId\":\"query-integration\"}"),
                    new JobExtensionRequest(
                            "company-" + sourceItemId,
                            "recruiter-" + sourceItemId,
                            company,
                            null,
                            null,
                            null,
                            "Internet",
                            salaryMinimum + "-" + salaryMaximum,
                            "SOURCE",
                            salaryMinimum,
                            salaryMaximum,
                            12,
                            city,
                            city,
                            null,
                            null,
                            "3-5年",
                            "本科",
                            null,
                            "HR",
                            null,
                            remoteType,
                            jobStatus,
                            "FETCHED",
                            OffsetDateTime.parse("2026-07-28T08:01:00Z"),
                            List.of("Java"),
                            List.of("Spring Boot"),
                            List.of("五险一金")),
                    objectMapper.readTree(
                            "{\"list\":{\"encrypt_job_id\":\""
                                    + sourceItemId
                                    + "\"},\"detail\":{\"jd\":\"sanitized\"}}"));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
