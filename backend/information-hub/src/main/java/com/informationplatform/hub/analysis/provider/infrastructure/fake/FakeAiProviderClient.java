package com.informationplatform.hub.analysis.provider.infrastructure.fake;

import com.informationplatform.hub.analysis.provider.application.AiProviderClient;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import com.informationplatform.hub.analysis.provider.domain.AiProviderResult;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsage;

/** CI 和上层测试可直接构造的确定性无网络 Provider。 */
public final class FakeAiProviderClient implements AiProviderClient {

    /** 每次调用返回的结构化文本。 */
    private final String outputText;

    /** 每次调用返回的确定性 Usage。 */
    private final AiProviderUsage usage;

    public FakeAiProviderClient() {
        this(
                "{\"fake\":true}",
                AiProviderUsage.reported(100L, 50L, 150L, null, null));
    }

    public FakeAiProviderClient(String outputText, AiProviderUsage usage) {
        if (outputText == null || usage == null) {
            throw new IllegalArgumentException("Fake AI Provider response must not be null");
        }
        this.outputText = outputText;
        this.usage = usage;
    }

    @Override
    public String providerId() {
        return "FAKE";
    }

    @Override
    public String modelName() {
        return "fake-model";
    }

    @Override
    public AiProviderResult execute(AiProviderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("AI Provider request must not be null");
        }
        return new AiProviderResult(
                providerId(),
                modelName(),
                "fake-request-1",
                outputText,
                "stop",
                0,
                usage);
    }
}
