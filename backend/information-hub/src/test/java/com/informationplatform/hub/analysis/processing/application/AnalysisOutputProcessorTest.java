package com.informationplatform.hub.analysis.processing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionValidationException;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceDefinition;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceInputProjector;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceOutput;
import com.informationplatform.hub.analysis.definition.job.JobUserRelevanceOutputValidator;
import com.informationplatform.hub.analysis.processing.domain.AnalysisOutputProcessingException;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessage;
import com.informationplatform.hub.analysis.provider.domain.AiProviderMessageRole;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;
import com.informationplatform.hub.analysis.provider.domain.AiProviderUsage;
import com.informationplatform.hub.analysis.provider.infrastructure.fake.FakeAiProviderClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisOutputProcessorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JobUserRelevanceDefinition definition = new JobUserRelevanceDefinition(
            objectMapper,
            new JobUserRelevanceInputProjector(),
            new JobUserRelevanceOutputValidator());
    private final AnalysisOutputProcessor processor = new AnalysisOutputProcessor(objectMapper);

    @Test
    void acceptsValidFakeResponseAndPreservesProviderUsage() throws Exception {
        ObjectNode output = validOutput();
        FakeAiProviderClient provider = new FakeAiProviderClient(
                objectMapper.writeValueAsString(output),
                AiProviderUsage.reported(120L, 30L, 150L, 10L, 5L));

        var providerResult = provider.execute(request());
        var validated = processor.process(definition, providerResult);

        assertThat(validated.value())
                .extracting(
                        JobUserRelevanceOutput::schemaVersion,
                        JobUserRelevanceOutput::relevanceScore,
                        JobUserRelevanceOutput::summary)
                .containsExactly(1, 80, "匹配度较高");
        assertThat(validated.providerResult()).isSameAs(providerResult);
        assertThat(validated.providerResult().usage().totalTokens()).isEqualTo(150);
        assertThat(validated.resultJson()).isEqualTo(output);

        ((ObjectNode) validated.resultJson()).put("relevanceScore", 1);
        assertThat(validated.resultJson().path("relevanceScore").intValue()).isEqualTo(80);
    }

    @Test
    void rejectsInvalidJsonMarkdownAndTrailingJsonWithoutEchoingResponse() {
        assertProcessingFailure(
                "{\"secret\":\"must-not-echo\"",
                "AI_RESPONSE_INVALID_JSON",
                "must-not-echo");
        assertProcessingFailure(
                "```json\n" + validOutput() + "\n```",
                "AI_RESPONSE_INVALID_JSON",
                "```json");
        assertProcessingFailure(
                validOutput() + "\n" + validOutput(),
                "AI_RESPONSE_INVALID_JSON",
                "\"relevanceScore\"");
    }

    @Test
    void rejectsDuplicateJsonFieldsAsAmbiguousOutput() throws Exception {
        String duplicateFields = objectMapper
                .writeValueAsString(validOutput())
                .replace(
                        "\"relevanceScore\":80",
                        "\"relevanceScore\":80,\"relevanceScore\":10");

        assertProcessingFailure(
                duplicateFields,
                "AI_RESPONSE_INVALID_JSON",
                "\"relevanceScore\"");
    }

    @Test
    void rejectsResponseAboveUtf8LimitBeforeJsonParsing() {
        String oversized = "界".repeat(AnalysisOutputProcessor.MAX_RESPONSE_BYTES / 3 + 1);

        assertThatThrownBy(() -> processor.process(
                        definition,
                        providerResult(oversized)))
                .isInstanceOfSatisfying(
                        AnalysisOutputProcessingException.class,
                        exception -> {
                            assertThat(exception.code()).isEqualTo("AI_RESPONSE_TOO_LARGE");
                            assertThat(exception.getMessage()).doesNotContain("界");
                        });
    }

    @Test
    void rejectsOutOfRangeScoresConfidenceAndSchemaLengthLimits() throws Exception {
        ObjectNode excessiveScore = validOutput();
        excessiveScore.put("relevanceScore", 101);
        assertDefinitionFailure(excessiveScore, "relevanceScore");

        ObjectNode excessiveConfidence = validOutput();
        excessiveConfidence.put("confidence", 1.01);
        assertDefinitionFailure(excessiveConfidence, "confidence");

        ObjectNode longSummary = validOutput();
        longSummary.put("summary", "字".repeat(1001));
        assertDefinitionFailure(longSummary, "1000 Unicode");

        ObjectNode tooManyItems = validOutput();
        ArrayNode items = tooManyItems.putArray("positiveSignals");
        for (int index = 0; index < 21; index++) {
            items.add("signal-" + index);
        }
        assertDefinitionFailure(tooManyItems, "at most 20");
    }

    @Test
    void leavesProviderUsageAvailableWhenDefinitionValidationFails() throws Exception {
        ObjectNode invalid = validOutput();
        invalid.put("relevanceScore", 101);
        var providerResult =
                providerResult(objectMapper.writeValueAsString(invalid));

        assertThatThrownBy(() -> processor.process(definition, providerResult))
                .isInstanceOf(AnalysisDefinitionValidationException.class);
        assertThat(providerResult.usage().inputTokens()).isEqualTo(10);
        assertThat(providerResult.usage().outputTokens()).isEqualTo(2);
        assertThat(providerResult.usage().totalTokens()).isEqualTo(12);
    }

    private void assertProcessingFailure(String output, String code, String secret) {
        assertThatThrownBy(() -> processor.process(definition, providerResult(output)))
                .isInstanceOfSatisfying(
                        AnalysisOutputProcessingException.class,
                        exception -> {
                            assertThat(exception.code()).isEqualTo(code);
                            assertThat(exception.getMessage()).doesNotContain(secret);
                            assertThat(exception.getCause()).isNull();
                        });
    }

    private void assertDefinitionFailure(ObjectNode output, String messagePart) throws Exception {
        assertThatThrownBy(() -> processor.process(
                        definition,
                        providerResult(objectMapper.writeValueAsString(output))))
                .isInstanceOf(AnalysisDefinitionValidationException.class)
                .hasMessageContaining(messagePart);
    }

    private com.informationplatform.hub.analysis.provider.domain.AiProviderResult providerResult(
            String output) {
        return new FakeAiProviderClient(output, AiProviderUsage.reported(10L, 2L, 12L, null, null))
                .execute(request());
    }

    private AiProviderRequest request() {
        return new AiProviderRequest(
                List.of(new AiProviderMessage(AiProviderMessageRole.USER, "test")),
                1000);
    }

    private ObjectNode validOutput() {
        ObjectNode output = objectMapper.createObjectNode();
        output.put("schemaVersion", 1);
        output.put("relevanceScore", 80);
        output.put("confidence", 0.9);
        output.put("summary", "匹配度较高");
        output.putArray("positiveSignals").add("Java");
        output.putArray("negativeSignals");
        output.putArray("attentionPoints");
        output.putArray("matchedPreferences").add("Java");
        output.putArray("unmatchedPreferences");
        return output;
    }
}
