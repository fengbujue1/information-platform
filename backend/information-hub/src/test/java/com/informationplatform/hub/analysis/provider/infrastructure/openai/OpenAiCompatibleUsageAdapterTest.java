package com.informationplatform.hub.analysis.provider.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsageStatus;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleUsageAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiCompatibleUsageAdapter adapter = new OpenAiCompatibleUsageAdapter();

    @Test
    void mapsPartialReportedUsageWithoutDerivingMissingActualTokens() throws Exception {
        var usage = adapter.adapt(objectMapper.readTree(
                """
                {
                  "prompt_tokens": 40,
                  "prompt_tokens_details": {"cached_tokens": 8}
                }
                """));

        assertThat(usage.status()).isEqualTo(AiProviderUsageStatus.REPORTED);
        assertThat(usage.inputTokens()).isEqualTo(40);
        assertThat(usage.cachedInputTokens()).isEqualTo(8);
        assertThat(usage.outputTokens()).isNull();
        assertThat(usage.totalTokens()).isNull();
        assertThat(usage.reasoningTokens()).isNull();
    }

    @Test
    void mapsMissingOrEmptyUsageToUnavailableAndRejectsInvalidTokens() throws Exception {
        assertThat(adapter.adapt(null).status()).isEqualTo(AiProviderUsageStatus.UNAVAILABLE);
        assertThat(adapter.adapt(objectMapper.readTree("{}")).status())
                .isEqualTo(AiProviderUsageStatus.UNAVAILABLE);
        assertThatThrownBy(() -> adapter.adapt(objectMapper.readTree(
                        "{\"completion_tokens\":-1}")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
