package com.informationplatform.hub.ingestion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.ingestion.api.dto.CollectorRequest;
import com.informationplatform.hub.ingestion.api.dto.InformationEnvelopeRequest;
import com.informationplatform.hub.ingestion.api.dto.IngestionResult;
import com.informationplatform.hub.ingestion.api.dto.JobExtensionRequest;
import com.informationplatform.hub.ingestion.infrastructure.persistence.InformationArchiveRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "INFORMATION_HUB_TEST_DB_URL",
        matches = "jdbc:mysql://.+")
class InformationIngestionIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private InformationArchiveRepository repository;

    @AfterEach
    void resetRepositorySpy() {
        reset(repository);
    }

    @Test
    @Transactional
    void firstSubmissionAndExactDuplicateAreIdempotent() {
        String sourceItemId = uniqueSourceItemId();
        InformationEnvelopeRequest request =
                request(sourceItemId, "版本 A", 20000, List.of("Java", "本科"), "FETCHED");

        IngestionResult created = ingestionService.ingest(request);
        IngestionResult repeated = ingestionService.ingest(request);

        assertTrue(created.created());
        assertEquals(1, created.versionNo());
        assertFalse(repeated.created());
        assertFalse(repeated.contentChanged());
        assertEquals(created.informationId(), repeated.informationId());
        assertEquals(1, repeated.versionNo());
        assertEquals(1, snapshotCount(created.informationId()));
        assertEquals(
                "recruiter-id",
                jdbcTemplate.queryForObject(
                        "SELECT source_recruiter_id FROM job_information WHERE information_id = ?",
                        String.class,
                        created.informationId()));
        assertEquals(
                "FETCHED",
                jdbcTemplate.queryForObject(
                        "SELECT detail_status FROM job_information WHERE information_id = ?",
                        String.class,
                        created.informationId()));
        assertEquals(
                "Java",
                jdbcTemplate.queryForObject(
                        """
                        SELECT JSON_UNQUOTE(JSON_EXTRACT(source_tags, '$[0]'))
                        FROM job_information
                        WHERE information_id = ?
                        """,
                        String.class,
                        created.informationId()));
        assertEquals(
                sourceItemId,
                jdbcTemplate.queryForObject(
                        """
                        SELECT JSON_UNQUOTE(
                            JSON_EXTRACT(raw_payload, '$.list.encrypt_job_id')
                        )
                        FROM information_item
                        WHERE id = ?
                        """,
                        String.class,
                        created.informationId()));
    }

    @Test
    @Transactional
    void contentCanChangeFromAToBToAWithIncreasingVersions() {
        String sourceItemId = uniqueSourceItemId();

        IngestionResult versionA = ingestionService.ingest(
                request(sourceItemId, "版本 A", 20000, List.of("Java"), "FETCHED"));
        IngestionResult versionB = ingestionService.ingest(
                request(sourceItemId, "版本 B", 20000, List.of("Java"), "FETCHED"));
        IngestionResult returnedToA = ingestionService.ingest(
                request(sourceItemId, "版本 A", 20000, List.of("Java"), "FETCHED"));

        assertEquals(1, versionA.versionNo());
        assertEquals(2, versionB.versionNo());
        assertEquals(3, returnedToA.versionNo());
        assertEquals(3, snapshotCount(versionA.informationId()));
        assertEquals(
                2,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(DISTINCT content_hash)
                        FROM information_snapshot
                        WHERE information_id = ?
                        """,
                        Integer.class,
                        versionA.informationId()));
    }

    @Test
    @Transactional
    void missingDetailsAndTagReorderingDoNotCreateVersionsButSalaryChangeDoes() {
        String sourceItemId = uniqueSourceItemId();
        IngestionResult initial = ingestionService.ingest(
                request(
                        sourceItemId,
                        "完整 JD",
                        20000,
                        List.of("Java", "MySQL"),
                        "FETCHED"));

        IngestionResult missingDetail = ingestionService.ingest(
                request(
                        sourceItemId,
                        null,
                        20000,
                        List.of("MySQL", "Java"),
                        "FAILED"));
        IngestionResult salaryChanged = ingestionService.ingest(
                request(
                        sourceItemId,
                        null,
                        25000,
                        List.of("Java", "MySQL"),
                        "FAILED"));

        assertFalse(missingDetail.contentChanged());
        assertEquals(1, missingDetail.versionNo());
        assertTrue(salaryChanged.contentChanged());
        assertEquals(2, salaryChanged.versionNo());
        assertEquals(
                "完整 JD",
                jdbcTemplate.queryForObject(
                        "SELECT content FROM information_item WHERE id = ?",
                        String.class,
                        initial.informationId()));
        assertEquals(2, snapshotCount(initial.informationId()));
    }

    @Test
    @Transactional
    void recruiterObservationUpdatesCurrentValueWithoutCreatingSnapshot() {
        String sourceItemId = uniqueSourceItemId();
        String firstObservation = "2026-07-27T05:30:00.123Z";
        String newerObservation = "2026-07-27T06:30:00.456Z";

        IngestionResult initial = ingestionService.ingest(request(
                sourceItemId,
                "完整 JD",
                20000,
                List.of("Java"),
                "FETCHED",
                firstObservation));
        IngestionResult missingObservation = ingestionService.ingest(request(
                sourceItemId,
                "完整 JD",
                20000,
                List.of("Java"),
                "FETCHED",
                null));

        assertFalse(missingObservation.contentChanged());
        assertEquals(1, missingObservation.versionNo());
        assertEquals(
                firstObservation,
                jdbcTemplate.queryForObject(
                        "SELECT recruiter_active_text FROM job_information WHERE information_id = ?",
                        String.class,
                        initial.informationId()));

        IngestionResult updatedObservation = ingestionService.ingest(request(
                sourceItemId,
                "完整 JD",
                20000,
                List.of("Java"),
                "FETCHED",
                newerObservation));

        assertFalse(updatedObservation.contentChanged());
        assertEquals(1, updatedObservation.versionNo());
        assertEquals(1, snapshotCount(initial.informationId()));
        assertEquals(
                newerObservation,
                jdbcTemplate.queryForObject(
                        "SELECT recruiter_active_text FROM job_information WHERE information_id = ?",
                        String.class,
                        initial.informationId()));
    }

    @Test
    void snapshotFailureRollsBackMainAndExtensionTables() {
        String sourceItemId = uniqueSourceItemId();
        doThrow(new IllegalStateException("simulated snapshot failure"))
                .when(repository)
                .insertSnapshot(anyLong(), anyInt(), any(), any());

        assertThrows(
                IllegalStateException.class,
                () -> ingestionService.ingest(
                        request(sourceItemId, "JD", 20000, List.of("Java"), "FETCHED")));

        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_item WHERE source_item_id = ?",
                        Integer.class,
                        sourceItemId));
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM job_information j
                        JOIN information_item i ON i.id = j.information_id
                        WHERE i.source_item_id = ?
                        """,
                        Integer.class,
                        sourceItemId));
    }

    private InformationEnvelopeRequest request(
            String sourceItemId,
            String content,
            Integer salaryMinimum,
            List<String> sourceTags,
            String detailStatus) {
        return request(
                sourceItemId,
                content,
                salaryMinimum,
                sourceTags,
                detailStatus,
                null);
    }

    private InformationEnvelopeRequest request(
            String sourceItemId,
            String content,
            Integer salaryMinimum,
            List<String> sourceTags,
            String detailStatus,
            String recruiterActiveText) {
        try {
            return new InformationEnvelopeRequest(
                    1,
                    "JOB",
                    "BOSS",
                    sourceItemId,
                    "https://example.test/jobs/" + sourceItemId,
                    "Java developer",
                    content,
                    null,
                    OffsetDateTime.parse("2026-07-26T08:00:00Z"),
                    new CollectorRequest("integration-test", "1.0"),
                    objectMapper.readTree("{\"runId\":\"integration\"}"),
                    new JobExtensionRequest(
                            "company-id",
                            "recruiter-id",
                            "Example company",
                            null,
                            null,
                            null,
                            "Internet",
                            salaryMinimum + "-30000",
                            "API",
                            salaryMinimum,
                            30000,
                            12,
                            "成都·武侯区·中和",
                            "成都",
                            "武侯区",
                            "中和",
                            "3-5年",
                            "本科",
                            null,
                            "HR",
                            recruiterActiveText,
                            "UNKNOWN",
                            "ACTIVE",
                            detailStatus,
                            null,
                            sourceTags,
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

    private int snapshotCount(long informationId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_snapshot WHERE information_id = ?",
                Integer.class,
                informationId);
    }

    private static String uniqueSourceItemId() {
        return "encrypt+/=_中文-" + UUID.randomUUID();
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
