package com.informationplatform.hub.analysis.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.informationplatform.hub.analysis.processing.domain.AnalysisTokenEstimate;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessage;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessageRole;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisTokenEstimatorTest {

    private final AnalysisTokenEstimator estimator = new AnalysisTokenEstimator();

    @Test
    void appliesFrozenUtf8FormulaToStableProviderMessages() {
        AiProviderRequest request = new AiProviderRequest(
                List.of(
                        new AiProviderMessage(AiProviderMessageRole.SYSTEM, "system"),
                        new AiProviderMessage(AiProviderMessageRole.USER, "关注 Java")),
                1000);

        AnalysisTokenEstimate estimate = estimator.estimate(request);

        long bytes = utf8("system\nsystem\n") + utf8("user\n关注 Java\n");
        long base = (bytes + 2) / 3;
        long expectedInput = ((base + 64) * 6 + 4) / 5;
        assertThat(estimate.inputTokens()).isEqualTo(expectedInput);
        assertThat(estimate.outputTokens()).isEqualTo(1000);
        assertThat(estimate.totalTokens()).isEqualTo(expectedInput + 1000);
        assertThat(estimate.method()).isEqualTo("UTF8_BYTES_DIV3_MARGIN20_V1");
    }

    private long utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
