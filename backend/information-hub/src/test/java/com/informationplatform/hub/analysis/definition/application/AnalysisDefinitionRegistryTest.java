package com.informationplatform.hub.analysis.definition.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceDefinition;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceDefinitionV2;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceInputProjector;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceOutputValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisDefinitionRegistryTest {

    private final JobUserRelevanceDefinition v1 = new JobUserRelevanceDefinition(
            new ObjectMapper(),
            new JobUserRelevanceInputProjector(),
            new JobUserRelevanceOutputValidator());
    private final JobUserRelevanceDefinition v2 = new JobUserRelevanceDefinitionV2(
        new ObjectMapper(),
        new JobUserRelevanceInputProjector(),
        new JobUserRelevanceOutputValidator());

    @Test
    void resolvesTheOnlyDefinitionByStableKeyAndVersion() {
        AnalysisDefinitionRegistry registry = new AnalysisDefinitionRegistry(List.of(v1,v2));

        assertThat(registry.require("JOB_USER_RELEVANCE", 1)).isSameAs(v1);
        assertThat(registry.require("JOB_USER_RELEVANCE", 2)).isSameAs(v2);
        assertThat(registry.requireCurrent("JOB_USER_RELEVANCE")).isSameAs(v2);
        assertThat(registry.all()).containsExactly(v1,v2);
    }

    @Test
    void rejectsUnknownAndDuplicateDefinitions() {
        AnalysisDefinitionRegistry registry = new AnalysisDefinitionRegistry(List.of(v2));

        assertThatThrownBy(() -> registry.require("JOB_USER_RELEVANCE", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Analysis Definition");
        assertThatThrownBy(() -> registry.requireCurrent("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Analysis Definition key");
        assertThatThrownBy(() -> new AnalysisDefinitionRegistry(List.of(v2, v2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate Analysis Definition");
    }
}
