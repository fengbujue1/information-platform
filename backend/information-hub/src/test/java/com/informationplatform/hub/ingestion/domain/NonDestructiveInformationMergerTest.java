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

    private ArchiveContent content(
            String body, List<String> tags, String detailStatus, String rawPayload)
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
                null,
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
