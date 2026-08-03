package com.informationplatform.hub.analysis.provider.infrastructure.fake;

import static org.assertj.core.api.Assertions.assertThat;

import com.informationplatform.hub.analysis.provider.domain.AiProviderMessage;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessageRole;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsage;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsageStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class FakeAiProviderClientTest {

    @Test
    void returnsDeterministicStructuredResponseAndReportedUsageWithoutNetwork() {
        FakeAiProviderClient client = new FakeAiProviderClient(
                "{\"schemaVersion\":1,\"relevanceScore\":80}",
                AiProviderUsage.reported(20L, 10L, 30L, 2L, 1L));
        AiProviderRequest request = new AiProviderRequest(
                List.of(new AiProviderMessage(AiProviderMessageRole.USER, "analyze")), 100);

        var first = client.execute(request);
        var second = client.execute(request);

        assertThat(first).isEqualTo(second);
        assertThat(first.provider()).isEqualTo("FAKE");
        assertThat(client.modelName()).isEqualTo("fake-model");
        assertThat(first.outputText()).contains("\"relevanceScore\":80");
        assertThat(first.usage().status()).isEqualTo(AiProviderUsageStatus.REPORTED);
        assertThat(first.usage().totalTokens()).isEqualTo(30);
    }
}
