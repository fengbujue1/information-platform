package com.informationplatform.hub.analysis.preview.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.preview.domain.PreviewCandidateEstimate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateFingerprintCalculatorTest {

    private final CandidateFingerprintCalculator calculator =
            new CandidateFingerprintCalculator(new ObjectMapper());

    @Test
    void fingerprintChangesWhenOrderEstimateOrSelectionDrifts() {
        PreviewCandidateEstimate first = candidate(1, 100, true);
        PreviewCandidateEstimate second = candidate(2, 200, false);
        String original = calculator.calculate(List.of(first, second));

        assertThat(calculator.calculate(List.of(first, second))).isEqualTo(original);
        assertThat(calculator.calculate(List.of(second, first))).isNotEqualTo(original);
        assertThat(calculator.calculate(List.of(candidate(1, 101, true), second)))
                .isNotEqualTo(original);
        assertThat(calculator.calculate(List.of(candidate(1, 100, false), second)))
                .isNotEqualTo(original);
    }

    private PreviewCandidateEstimate candidate(
            long informationId,
            long estimatedInputTokens,
            boolean selected) {
        return new PreviewCandidateEstimate(
                informationId,
                informationId + 100,
                LocalDateTime.of(2026, 8, 5, 1, 2, 3),
                estimatedInputTokens,
                1_000,
                estimatedInputTokens + 1_000,
                selected);
    }
}
