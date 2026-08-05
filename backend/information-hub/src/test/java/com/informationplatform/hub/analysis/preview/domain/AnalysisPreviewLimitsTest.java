package com.informationplatform.hub.analysis.preview.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewRequestException;
import org.junit.jupiter.api.Test;

class AnalysisPreviewLimitsTest {

    @Test
    void appliesFrozenDefaults() {
        assertThat(AnalysisPreviewLimits.resolve(null, null, null))
                .isEqualTo(new AnalysisPreviewLimits(3, 20, 75_000));
    }

    @Test
    void acceptsHardCapsAndRejectsNonPositiveValues() {
        assertThat(AnalysisPreviewLimits.resolve(14, 50, 200_000L))
                .isEqualTo(new AnalysisPreviewLimits(14, 50, 200_000));

        assertThatThrownBy(() -> AnalysisPreviewLimits.resolve(0, 20, 75_000L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
        assertThatThrownBy(() -> AnalysisPreviewLimits.resolve(3, 0, 75_000L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
        assertThatThrownBy(() -> AnalysisPreviewLimits.resolve(3, 20, 0L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
    }

    @Test
    void rejectsValuesAboveHardCaps() {
        assertThatThrownBy(() -> AnalysisPreviewLimits.resolve(15, 20, 75_000L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
        assertThatThrownBy(() -> AnalysisPreviewLimits.resolve(3, 51, 75_000L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
        assertThatThrownBy(() -> AnalysisPreviewLimits.resolve(3, 20, 200_001L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
    }
}
