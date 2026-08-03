package com.informationplatform.hub.analysis.processing.application;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.processing.domain.AnalysisOutputProcessingException;
import com.informationplatform.hub.analysis.processing.domain.ValidatedAnalysisOutput;
import com.informationplatform.hub.analysis.provider.domain.AiProviderResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class AnalysisOutputProcessor {

    /** Provider Assistant 文本允许进入 JSON 解析的最大 UTF-8 大小：1 MiB。 */
    public static final int MAX_RESPONSE_BYTES = 1_048_576;

    /** 现有平台 Jackson 实例。 */
    private final ObjectMapper objectMapper;

    public AnalysisOutputProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 严格解析一个且仅一个 JSON 值，并交由 Definition 执行冻结 Schema 语义校验。
     *
     * <p>异常不保留 Jackson cause 或 Provider 原始响应；调用方仍持有 AiProviderResult，可在校验失败时保存
     * Provider 已报告的 Actual Usage。
     */
    public <O> ValidatedAnalysisOutput<O> process(
            AnalysisDefinition<?, O> definition, AiProviderResult providerResult) {
        if (definition == null || providerResult == null) {
            throw new IllegalArgumentException("Analysis output inputs must not be null");
        }
        String outputText = providerResult.outputText();
        if (outputText.getBytes(StandardCharsets.UTF_8).length > MAX_RESPONSE_BYTES) {
            throw invalid(
                    "AI_RESPONSE_TOO_LARGE",
                    "AI Provider response exceeds the platform size limit");
        }

        JsonNode output = parseSingleJson(outputText);
        O validated = definition.validateOutput(output);
        return new ValidatedAnalysisOutput<>(output, validated, providerResult);
    }

    /** Jackson 默认可接受尾随内容，因此显式确认首个 JSON 后没有第二个 Token。 */
    private JsonNode parseSingleJson(String outputText) {
        try (JsonParser parser = objectMapper.createParser(outputText)) {
            // 重复字段会产生“前值还是后值生效”的歧义，严格输出契约必须在构造 JsonNode 前拒绝。
            parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            JsonNode output = objectMapper.readTree(parser);
            if (output == null) {
                throw invalid("AI_RESPONSE_INVALID_JSON", "AI Provider response must contain JSON");
            }
            if (parser.nextToken() != null) {
                throw invalid(
                        "AI_RESPONSE_INVALID_JSON",
                        "AI Provider response must contain exactly one JSON value");
            }
            return output;
        } catch (AnalysisOutputProcessingException exception) {
            throw exception;
        } catch (IOException exception) {
            // Jackson 异常可能包含原始响应片段，因此只抛出稳定脱敏错误。
            throw invalid("AI_RESPONSE_INVALID_JSON", "AI Provider response is not valid JSON");
        }
    }

    private AnalysisOutputProcessingException invalid(String code, String message) {
        return new AnalysisOutputProcessingException(code, message);
    }
}
