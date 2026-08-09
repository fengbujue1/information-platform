package com.informationplatform.hub.analysis.provider.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AiProviderPropertiesTest {

    @Test
    void defaultsToDisabledAndNeverPrintsSecretsOrRawBaseUrl() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setApiKey("test-secret-key");
        properties.setBaseUrl("https://user:password@provider.test/v1");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.getMaxOutputTokens()).isEqualTo(5000);
        assertThat(properties.toString())
                .contains("baseUrlConfigured=true")
                .contains("apiKey='<redacted>'")
                .doesNotContain("test-secret-key")
                .doesNotContain("user:password")
                .doesNotContain("provider.test");
    }

    @Test
    void rejectsInvalidTimeoutAndOutputLimit() {
        AiProviderProperties properties = new AiProviderProperties();

        assertThatThrownBy(() -> properties.setTimeout(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties.setMaxOutputTokens(5001))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
