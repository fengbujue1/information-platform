package com.informationplatform.hub.analysis.processing.application;

import com.informationplatform.hub.analysis.processing.domain.AnalysisTokenEstimate;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessage;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/** 实现 Accepted UTF8_BYTES_DIV3_MARGIN20_V1 单条 Token Estimate。 */
@Component
public class AnalysisTokenEstimator {

    /** 冻结估算算法标识，供 TASK-028 Preview 和预算复用。 */
    public static final String METHOD = "UTF8_BYTES_DIV3_MARGIN20_V1";

    /** 对稳定三消息的角色、换行和正文 UTF-8 字节执行冻结公式。 */
    public AnalysisTokenEstimate estimate(AiProviderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("AI Provider request must not be null");
        }
        long bytes = 0;
        for (AiProviderMessage message : request.messages()) {
            bytes = Math.addExact(bytes, utf8Bytes(message.role().wireValue()));
            bytes = Math.addExact(bytes, 1);
            bytes = Math.addExact(bytes, utf8Bytes(message.content()));
            bytes = Math.addExact(bytes, 1);
        }
        long baseTokens = divideRoundUp(bytes, 3);
        long estimatedInput = divideRoundUp(Math.multiplyExact(baseTokens + 64, 6), 5);
        long estimatedOutput = request.maxOutputTokens();
        return new AnalysisTokenEstimate(
                estimatedInput,
                estimatedOutput,
                Math.addExact(estimatedInput, estimatedOutput),
                METHOD);
    }

    private long utf8Bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private long divideRoundUp(long value, long divisor) {
        return Math.floorDiv(Math.addExact(value, divisor - 1), divisor);
    }
}
