package com.informationplatform.hub.ingestion.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalContentHasherTest {

    private final CanonicalContentHasher hasher =
            new CanonicalContentHasher(new ObjectMapper());

    @Test
    void tagOrderAndCollectionMetadataDoNotChangeHash() {
        ArchiveContent first = content(
                "A",
                20000,
                "API",
                "FETCHED",
                List.of("Java", "MySQL"),
                "{\"runId\":\"one\"}");
        ArchiveContent second = content(
                "A",
                20000,
                "HTML",
                "FAILED",
                List.of("MySQL", "Java", "Java"),
                "{\"runId\":\"two\"}");

        assertEquals(hasher.fingerprint(first).hash(), hasher.fingerprint(second).hash());
    }

    @Test
    void businessChangeProducesNewHashAndReturningToAProducesOriginalHash() {
        ArchiveContent versionA =
                content("A", 20000, "API", "FETCHED", List.of("Java"), "{}");
        ArchiveContent versionB =
                content("B", 25000, "API", "FETCHED", List.of("Java"), "{}");
        ArchiveContent returnedToA =
                content("A", 20000, "API", "FETCHED", List.of("Java"), "{\"new\":true}");

        String hashA = hasher.fingerprint(versionA).hash();
        String hashB = hasher.fingerprint(versionB).hash();

        assertNotEquals(hashA, hashB);
        assertEquals(hashA, hasher.fingerprint(returnedToA).hash());
    }

    private ArchiveContent content(
            String body,
            Integer minimumSalary,
            String salarySource,
            String detailStatus,
            List<String> sourceTags,
            String collectionContext) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            InformationFields information = new InformationFields(
                    1,
                    "JOB",
                    "BOSS",
                    "encrypt-id",
                    "https://example.test/job",
                    "Java developer",
                    body,
                    null,
                    Instant.parse("2026-07-26T08:00:00Z"),
                    "collector",
                    "1.0",
                    objectMapper.readTree(collectionContext),
                    objectMapper.readTree("{\"raw\":\"latest\"}"));
            JobFields job = new JobFields(
                    "company",
                    "recruiter",
                    "Example",
                    null,
                    null,
                    null,
                    null,
                    "20-30K",
                    salarySource,
                    minimumSalary,
                    30000,
                    12,
                    "成都·武侯区",
                    "成都",
                    "武侯区",
                    null,
                    "3-5年",
                    "本科",
                    null,
                    null,
                    null,
                    "UNKNOWN",
                    "ACTIVE",
                    detailStatus,
                    Instant.parse("2026-07-26T08:00:00Z"),
                    sourceTags,
                    List.of("Spring Boot"),
                    List.of("五险一金"));
            return new ArchiveContent(information, job);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
