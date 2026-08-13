package com.informationplatform.hub.analysis.preview.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AnalysisPreviewPropertiesTest {

    @Test
    void acceptsPositiveOrderedDefaultsAndMaxima() {
        AnalysisPreviewProperties properties = new AnalysisPreviewProperties();
        properties.setDefaultMaxCandidates(60);
        properties.setMaxCandidates(100);

        assertThatCode(properties::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void rejectsDefaultAboveMaximumAtStartup() {
        AnalysisPreviewProperties properties = new AnalysisPreviewProperties();
        properties.setDefaultWindowDays(15);
        properties.setMaxWindowDays(14);

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("windowDays default must not exceed maximum");
    }

    @Test
    void rejectsValuesAboveTechnicalSafetyBoundaryAtStartup() {
        AnalysisPreviewProperties properties = new AnalysisPreviewProperties();
        properties.setMaxCandidates(10_001);

        assertThatThrownBy(properties::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("technical safety boundary");
    }
}
