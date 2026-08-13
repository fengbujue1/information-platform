package com.informationplatform.hub.analysis.preview.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewLimitPolicy;
import com.informationplatform.hub.analysis.preview.application.AnalysisPreviewRequestException;
import com.informationplatform.hub.analysis.preview.infrastructure.config.AnalysisPreviewProperties;
import org.junit.jupiter.api.Test;

class AnalysisPreviewLimitsTest {

    @Test
    void appliesConfiguredDefaults() {
        AnalysisPreviewLimitPolicy policy = policy(new AnalysisPreviewProperties());

        assertThat(policy.resolve(null, null, null))
                .isEqualTo(new AnalysisPreviewLimits(3, 20, 75_000));
        assertThat(policy.metadata().maxCandidates().maximum()).isEqualTo(50);
    }

    @Test
    void acceptsConfiguredMaximaAndRejectsNonPositiveValues() {
        AnalysisPreviewProperties properties = new AnalysisPreviewProperties();
        properties.setMaxWindowDays(30);
        properties.setMaxCandidates(100);
        properties.setMaxEstimatedTokens(500_000);
        AnalysisPreviewLimitPolicy policy = policy(properties);

        assertThat(policy.resolve(30, 100, 500_000L))
                .isEqualTo(new AnalysisPreviewLimits(30, 100, 500_000));
        assertThatThrownBy(() -> policy.resolve(0, 20, 75_000L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
        assertThatThrownBy(() -> policy.resolve(3, 0, 75_000L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
        assertThatThrownBy(() -> policy.resolve(3, 20, 0L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
    }

    @Test
    void rejectsValuesAboveConfiguredMaxima() {
        AnalysisPreviewLimitPolicy policy = policy(new AnalysisPreviewProperties());

        assertThatThrownBy(() -> policy.resolve(15, 20, 75_000L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
        assertThatThrownBy(() -> policy.resolve(3, 51, 75_000L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
        assertThatThrownBy(() -> policy.resolve(3, 20, 200_001L))
                .isInstanceOf(AnalysisPreviewRequestException.class);
    }

    private AnalysisPreviewLimitPolicy policy(AnalysisPreviewProperties properties) {
        properties.afterPropertiesSet();
        return new AnalysisPreviewLimitPolicy(properties);
    }
}
