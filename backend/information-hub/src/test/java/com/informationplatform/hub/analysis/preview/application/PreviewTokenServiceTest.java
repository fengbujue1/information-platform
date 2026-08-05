package com.informationplatform.hub.analysis.preview.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.informationplatform.hub.analysis.preview.domain.PreviewTokenPayload;
import com.informationplatform.hub.analysis.preview.infrastructure.config.AnalysisPreviewProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PreviewTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-05T06:00:00Z");

    @Test
    void signsVerifiesAndRejectsTampering() {
        PreviewTokenService service = service(NOW);
        PreviewTokenPayload payload = payload(NOW);

        String token = service.issue(payload);

        assertThat(service.verify(token)).isEqualTo(payload);
        assertThat(service.verifyForOwner(token, 7)).isEqualTo(payload);
        assertThatThrownBy(() -> service.verifyForOwner(token, 8))
                .isInstanceOf(AnalysisPreviewConflictException.class);
        String tampered = (token.charAt(0) == 'A' ? "B" : "A") + token.substring(1);
        assertThatThrownBy(() -> service.verify(tampered))
                .isInstanceOf(AnalysisPreviewConflictException.class)
                .hasMessage("Preview Token is invalid");
    }

    @Test
    void rejectsExpiredTokenAndMissingSecret() {
        String token = service(NOW).issue(payload(NOW));
        assertThatThrownBy(() -> service(NOW.plusSeconds(601)).verify(token))
                .isInstanceOf(AnalysisPreviewConflictException.class)
                .hasMessage("Preview Token has expired");

        AnalysisPreviewProperties properties = new AnalysisPreviewProperties();
        PreviewTokenService missingSecret = new PreviewTokenService(
                mapper(), properties, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> missingSecret.issue(payload(NOW)))
                .isInstanceOf(AnalysisPreviewConfigurationException.class);
    }

    @Test
    void rejectsMalformedTokenAndShortSecret() {
        assertThatThrownBy(() -> service(NOW).verify("not-a-signed-token"))
                .isInstanceOfSatisfying(
                        AnalysisPreviewConflictException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("PREVIEW_TOKEN_INVALID"));

        AnalysisPreviewProperties properties = new AnalysisPreviewProperties();
        properties.setHmacSecret("too-short");
        PreviewTokenService shortSecret = new PreviewTokenService(
                mapper(), properties, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> shortSecret.issue(payload(NOW)))
                .isInstanceOf(AnalysisPreviewConfigurationException.class)
                .hasMessageContaining("at least 32 UTF-8 bytes");
    }

    private PreviewTokenService service(Instant now) {
        AnalysisPreviewProperties properties = new AnalysisPreviewProperties();
        properties.setHmacSecret("task-028-test-secret-with-at-least-32-bytes");
        return new PreviewTokenService(
                mapper(), properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    private ObjectMapper mapper() {
        return JsonMapper.builder().addModule(new JavaTimeModule()).build();
    }

    private PreviewTokenPayload payload(Instant now) {
        return new PreviewTokenPayload(
                1, 7, 11, 12, "JOB_USER_RELEVANCE", 1,
                now.minusSeconds(259_200), now, 3, 20, 75_000,
                4, 3, 1, 2, 1, 0,
                200, 2_000, 2_200, "UTF8_BYTES_DIV3_MARGIN20_V1",
                "a".repeat(64), "request-1", now, now.plusSeconds(600));
    }
}
