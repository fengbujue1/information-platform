package com.informationplatform.hub.analysis.definition.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceDefinition;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceInputProjector;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceOutputValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisDefinitionRegistryTest {

    private final JobUserRelevanceDefinition definition = new JobUserRelevanceDefinition(
            new ObjectMapper(),
            new JobUserRelevanceInputProjector(),
            new JobUserRelevanceOutputValidator());

    @Test
    void resolvesTheOnlyDefinitionByStableKeyAndVersion() {
        AnalysisDefinitionRegistry registry = new AnalysisDefinitionRegistry(List.of(definition));

        assertThat(registry.require("JOB_USER_RELEVANCE", 1)).isSameAs(definition);
        assertThat(registry.requireCurrent("JOB_USER_RELEVANCE")).isSameAs(definition);
        assertThat(registry.all()).containsExactly(definition);
    }

    @Test
    void rejectsUnknownAndDuplicateDefinitions() {
        AnalysisDefinitionRegistry registry = new AnalysisDefinitionRegistry(List.of(definition));

        assertThatThrownBy(() -> registry.require("JOB_USER_RELEVANCE", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Analysis Definition");
        assertThatThrownBy(() -> registry.requireCurrent("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Analysis Definition key");
        assertThatThrownBy(() -> new AnalysisDefinitionRegistry(List.of(definition, definition)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate Analysis Definition");
    }
}
