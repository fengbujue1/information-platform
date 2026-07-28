package com.informationplatform.hub.job.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.job.api.dto.JobDetailResponse;
import com.informationplatform.hub.job.api.dto.JobQueryRequest;
import com.informationplatform.hub.job.api.dto.JobSnapshotResponse;
import com.informationplatform.hub.job.api.dto.PageResponse;
import com.informationplatform.hub.job.infrastructure.persistence.query.JobQueryMapper;
import com.informationplatform.hub.job.infrastructure.persistence.query.JobQueryRow;
import com.informationplatform.hub.job.infrastructure.persistence.query.JobSnapshotQueryRow;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JobQueryServiceTest {

    @Mock
    private JobQueryMapper queryMapper;

    private JobQueryService service;

    @BeforeEach
    void setUp() {
        service = new JobQueryService(queryMapper, new ObjectMapper());
    }

    @Test
    void appliesDefaultsAndReturnsStablePage() {
        when(queryMapper.countJobs(any())).thenReturn(1L);
        when(queryMapper.selectJobs(any())).thenReturn(List.of(listRow()));

        PageResponse<?> response = service.listJobs(emptyRequest());

        ArgumentCaptor<JobQueryCriteria> criteria =
                ArgumentCaptor.forClass(JobQueryCriteria.class);
        verify(queryMapper).selectJobs(criteria.capture());
        assertEquals(1, criteria.getValue().page());
        assertEquals(20, criteria.getValue().size());
        assertEquals(0, criteria.getValue().offset());
        assertEquals(JobSortField.FIRST_SEEN_TIME, criteria.getValue().sortField());
        assertEquals(SortDirection.DESC, criteria.getValue().sortDirection());
        assertEquals(1, response.total());
        assertEquals(1, response.totalPages());
        assertEquals(1, response.items().size());
    }

    @Test
    void normalizesFiltersAndUsesSalaryOverlapCriteria() {
        when(queryMapper.countJobs(any())).thenReturn(0L);
        JobQueryRequest request = new JobQueryRequest(
                3,
                25,
                " Java ",
                " Example ",
                " 成都 ",
                20000,
                30000,
                " BOSS ",
                " ACTIVE ",
                " REMOTE ",
                "lastSeenTime",
                "asc");

        service.listJobs(request);

        ArgumentCaptor<JobQueryCriteria> criteria =
                ArgumentCaptor.forClass(JobQueryCriteria.class);
        verify(queryMapper).countJobs(criteria.capture());
        JobQueryCriteria actual = criteria.getValue();
        assertEquals(50, actual.offset());
        assertEquals("Java", actual.keyword());
        assertEquals("Example", actual.company());
        assertEquals("成都", actual.city());
        assertEquals(20000, actual.salaryMin());
        assertEquals(30000, actual.salaryMax());
        assertEquals("BOSS", actual.source());
        assertEquals("ACTIVE", actual.jobStatus());
        assertEquals("REMOTE", actual.remoteType());
        assertEquals(JobSortField.LAST_SEEN_TIME, actual.sortField());
        assertEquals(SortDirection.ASC, actual.sortDirection());
        verify(queryMapper, never()).selectJobs(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "firstSeenTime",
        "lastSeenTime",
        "publishTime",
        "salaryMinMonthlyYuan"
    })
    void acceptsOnlyDocumentedSortFields(String sortField) {
        when(queryMapper.countJobs(any())).thenReturn(0L);

        service.listJobs(requestWithSort(sortField, "desc"));

        verify(queryMapper).countJobs(any());
    }

    @Test
    void rejectsInvalidPaginationSortingAndSalary() {
        assertThrows(
                JobQueryRequestException.class,
                () -> service.listJobs(requestWithPage(0, 20)));
        assertThrows(
                JobQueryRequestException.class,
                () -> service.listJobs(requestWithPage(1, 101)));
        assertThrows(
                JobQueryRequestException.class,
                () -> service.listJobs(requestWithSort("title; DROP TABLE", "desc")));
        assertThrows(
                JobQueryRequestException.class,
                () -> service.listJobs(requestWithSort("firstSeenTime", "sideways")));
        assertThrows(
                JobQueryRequestException.class,
                () -> service.listJobs(new JobQueryRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        30000,
                        20000,
                        null,
                        null,
                        null,
                        null,
                        null)));
    }

    @Test
    void mapsDetailJsonAndUtcTimesWithoutRawPayload() {
        JobQueryRow row = detailRow();
        when(queryMapper.selectJobById(7L)).thenReturn(row);

        JobDetailResponse response = service.getJob(7L);

        assertEquals(7, response.id());
        assertEquals("Example company", response.companyName());
        assertEquals("Java", response.sourceTags().get(0).asText());
        assertEquals(Instant.parse("2026-07-28T01:02:03Z"), response.collectedAt());
        assertNull(response.publishTime());
    }

    @Test
    void rejectsInvalidOrMissingJob() {
        assertThrows(JobQueryRequestException.class, () -> service.getJob(0));
        when(queryMapper.selectJobById(404L)).thenReturn(null);

        assertThrows(JobNotFoundException.class, () -> service.getJob(404L));
    }

    @Test
    void returnsSnapshotsInMapperOrderAfterCheckingJobExists() {
        when(queryMapper.selectJobById(7L)).thenReturn(detailRow());
        JobSnapshotQueryRow newer = snapshotRow(2, "2026-07-28T02:00:00");
        JobSnapshotQueryRow older = snapshotRow(1, "2026-07-28T01:00:00");
        when(queryMapper.selectSnapshots(7L)).thenReturn(List.of(newer, older));

        List<JobSnapshotResponse> response = service.getSnapshots(7L);

        assertEquals(List.of(2, 1), response.stream()
                .map(JobSnapshotResponse::versionNo)
                .toList());
        assertEquals("JOB", response.getFirst()
                .standardizedPayload()
                .get("informationType")
                .asText());
    }

    private JobQueryRequest emptyRequest() {
        return new JobQueryRequest(
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private JobQueryRequest requestWithPage(int page, int size) {
        return new JobQueryRequest(
                page, size, null, null, null, null, null, null, null, null, null, null);
    }

    private JobQueryRequest requestWithSort(String sortBy, String direction) {
        return new JobQueryRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                sortBy,
                direction);
    }

    private JobQueryRow listRow() {
        JobQueryRow row = new JobQueryRow();
        row.setId(7L);
        row.setSource("BOSS");
        row.setSourceItemId("source-7");
        row.setTitle("Java developer");
        row.setCompanyName("Example company");
        row.setCurrentVersionNo(1);
        row.setFirstSeenTime(LocalDateTime.parse("2026-07-28T01:00:00"));
        row.setLastSeenTime(LocalDateTime.parse("2026-07-28T02:00:00"));
        return row;
    }

    private JobQueryRow detailRow() {
        JobQueryRow row = listRow();
        row.setContent("Job description");
        row.setCollectedAt(LocalDateTime.parse("2026-07-28T01:02:03"));
        row.setSourceTags("[\"Java\"]");
        row.setSourceSkillTags("[\"Spring Boot\"]");
        row.setWelfare("[\"五险一金\"]");
        return row;
    }

    private JobSnapshotQueryRow snapshotRow(int version, String createdAt) {
        JobSnapshotQueryRow row = new JobSnapshotQueryRow();
        row.setId((long) version);
        row.setVersionNo(version);
        row.setContentHash("a".repeat(64));
        row.setTitle("Java developer");
        row.setContent("Job description");
        row.setStandardizedPayload("{\"informationType\":\"JOB\"}");
        row.setCollectedAt(LocalDateTime.parse(createdAt));
        row.setCollectorId("collector");
        row.setCollectorVersion("1.0");
        row.setCreatedAt(LocalDateTime.parse(createdAt));
        return row;
    }
}
