package com.informationplatform.hub.analysis.provider.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiProviderDomainTest {

    @Test
    void requestDefensivelyCopiesMessagesAndEnforcesPlatformTokenCap() {
        List<AiProviderMessage> messages =
                new ArrayList<>(List.of(new AiProviderMessage(AiProviderMessageRole.USER, "hello")));

        AiProviderRequest request = new AiProviderRequest(messages, 5000);
        messages.clear();

        assertThat(request.messages()).hasSize(1);
        assertThatThrownBy(() -> new AiProviderRequest(request.messages(), 5001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 5000");
    }

    @Test
    void unavailableUsageCannotContainDerivedTokenValues() {
        AiProviderUsage unavailable = AiProviderUsage.unavailable();

        assertThat(unavailable.status()).isEqualTo(AiProviderUsageStatus.UNAVAILABLE);
        assertThat(unavailable.inputTokens()).isNull();
        assertThat(unavailable.outputTokens()).isNull();
        assertThat(unavailable.totalTokens()).isNull();
        assertThatThrownBy(() -> new AiProviderUsage(
                        1L, null, null, null, null, AiProviderUsageStatus.UNAVAILABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain");
        assertThatThrownBy(() -> AiProviderUsage.reported(null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one");
    }
}
