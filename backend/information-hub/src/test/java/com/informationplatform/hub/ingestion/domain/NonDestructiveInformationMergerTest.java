package com.informationplatform.hub.ingestion.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class NonDestructiveInformationMergerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NonDestructiveInformationMerger merger =
            new NonDestructiveInformationMerger();

    @Test
    void nullContentAndEmptyArraysDoNotDestroyExistingValues() throws Exception {
        ArchiveContent current = content(
                "完整 JD",
                List.of("Java", "MySQL"),
                "FETCHED",
                "{\"attempt\":1}");
        ArchiveContent incoming = content(null, List.of(), "FAILED", "{\"attempt\":2}");

        ArchiveContent merged = merger.merge(current, incoming);

        assertEquals("完整 JD", merged.information().content());
        assertEquals(List.of("Java", "MySQL"), merged.job().sourceTags());
        assertEquals("FAILED", merged.job().detailStatus());
        assertEquals(2, merged.information().rawPayload().get("attempt").intValue());
    }

    @Test
    void recruiterObservationRetainsOldValueWhenMissingAndAcceptsNewValue() throws Exception {
        ArchiveContent current = content(
                "JD",
                List.of("Java"),
                "FETCHED",
                "{\"attempt\":1}",
                "2026-07-27T05:30:00.123Z");
        ArchiveContent missingObservation =
                content("JD", List.of("Java"), "FETCHED", "{\"attempt\":2}", null);
        ArchiveContent newerObservation = content(
                "JD",
                List.of("Java"),
                "FETCHED",
                "{\"attempt\":3}",
                "2026-07-27T06:30:00.456Z");

        ArchiveContent retained = merger.merge(current, missingObservation);
        ArchiveContent replaced = merger.merge(retained, newerObservation);

        assertEquals(
                "2026-07-27T05:30:00.123Z",
                retained.job().recruiterActiveText());
        assertEquals(
                "2026-07-27T06:30:00.456Z",
                replaced.job().recruiterActiveText());
    }

    private ArchiveContent content(
            String body, List<String> tags, String detailStatus, String rawPayload)
            throws Exception {
        return content(body, tags, detailStatus, rawPayload, null);
    }

    private ArchiveContent content(
            String body,
            List<String> tags,
            String detailStatus,
            String rawPayload,
            String recruiterActiveText)
            throws Exception {
        InformationFields information = new InformationFields(
                1,
                "JOB",
                "BOSS",
                "id",
                null,
                "Title",
                body,
                null,
                Instant.parse("2026-07-26T08:00:00Z"),
                "collector",
                "1",
                null,
                objectMapper.readTree(rawPayload));
        JobFields job = new JobFields(
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
                recruiterActiveText,
                "UNKNOWN",
                "ACTIVE",
                detailStatus,
                null,
                tags,
                null,
                null);
        return new ArchiveContent(information, job);
    }
}
