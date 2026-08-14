package com.informationplatform.hub.recommendation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** 锁定已经执行的 V3 migration 内容，防止通用化纠偏误改历史 migration。 */
class V3MigrationImmutabilityTest {

    private static final String ACCEPTED_V3_SHA256 =
            "cf5ff78fe0421879f6151e3034b3b71ebe9db3b7d413b8a33c02205fc1773aae";

    @Test
    void v3MigrationChecksumRemainsUnchanged() throws Exception {
        String content = Files.readString(Path.of(
                "src/main/resources/db/migration/V3__create_phase4_recommendation_tables.sql"),
                StandardCharsets.UTF_8);
        String normalizedContent = content.replace("\r\n", "\n").replace('\r', '\n');
        String actual = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(normalizedContent.getBytes(StandardCharsets.UTF_8)));

        assertThat(actual).isEqualTo(ACCEPTED_V3_SHA256);
    }
}
