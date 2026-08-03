package com.informationplatform.hub.analysis.definition.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionValidationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class JobUserRelevanceOutputValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JobUserRelevanceOutputValidator validator = new JobUserRelevanceOutputValidator();

    @Test
    void acceptsValidOutputIncludingScoreAndConfidenceBoundaries() {
        ObjectNode minimum = validOutput();
        minimum.put("relevanceScore", 0);
        minimum.put("confidence", 0);
        ObjectNode maximum = validOutput();
        maximum.put("relevanceScore", 100);
        maximum.put("confidence", 1);

        JobUserRelevanceOutput minimumResult = validator.validate(minimum);
        JobUserRelevanceOutput maximumResult = validator.validate(maximum);

        assertThat(minimumResult.relevanceScore()).isZero();
        assertThat(minimumResult.confidence()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(maximumResult.relevanceScore()).isEqualTo(100);
        assertThat(maximumResult.confidence()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void rejectsMissingAndUnknownFields() {
        ObjectNode missing = validOutput();
        missing.remove("summary");
        ObjectNode unknown = validOutput();
        unknown.put("explanation", "extra");

        assertInvalid(missing, "exactly match");
        assertInvalid(unknown, "exactly match");
    }

    @Test
    void rejectsWrongTypesAndOutOfRangeNumbers() {
        ObjectNode fractionalScore = validOutput();
        fractionalScore.put("relevanceScore", 50.5);
        ObjectNode excessiveScore = validOutput();
        excessiveScore.put("relevanceScore", 101);
        ObjectNode negativeScore = validOutput();
        negativeScore.put("relevanceScore", -1);
        ObjectNode negativeConfidence = validOutput();
        negativeConfidence.put("confidence", -0.01);
        ObjectNode excessiveConfidence = validOutput();
        excessiveConfidence.put("confidence", 1.01);
        ObjectNode wrongSchemaVersion = validOutput();
        wrongSchemaVersion.put("schemaVersion", 2);

        assertInvalid(fractionalScore, "must be an integer");
        assertInvalid(excessiveScore, "between 0 and 100");
        assertInvalid(negativeScore, "between 0 and 100");
        assertInvalid(negativeConfidence, "between 0 and 1");
        assertInvalid(excessiveConfidence, "between 0 and 1");
        assertInvalid(wrongSchemaVersion, "schemaVersion must be 1");
    }

    @Test
    void validatesUnicodeLengthsArraySizesAndArrayItemTypes() {
        ObjectNode validUnicodeSummary = validOutput();
        validUnicodeSummary.put("summary", "😀".repeat(1000));
        assertThat(validator.validate(validUnicodeSummary).summary()).hasSize(2000);

        ObjectNode longSummary = validOutput();
        longSummary.put("summary", "😀".repeat(1001));
        assertInvalid(longSummary, "1000 Unicode");

        ObjectNode tooManyItems = validOutput();
        ArrayNode items = tooManyItems.putArray("positiveSignals");
        for (int index = 0; index < 21; index++) {
            items.add("signal-" + index);
        }
        assertInvalid(tooManyItems, "at most 20");

        ObjectNode longItem = validOutput();
        longItem.withArray("negativeSignals").add("字".repeat(501));
        assertInvalid(longItem, "500 Unicode");

        ObjectNode wrongItemType = validOutput();
        wrongItemType.withArray("attentionPoints").add(1);
        assertInvalid(wrongItemType, "only strings");
    }

    private ObjectNode validOutput() {
        ObjectNode output = objectMapper.createObjectNode();
        output.put("schemaVersion", 1);
        output.put("relevanceScore", 75);
        output.put("confidence", 0.8);
        output.put("summary", "匹配度较高");
        output.putArray("positiveSignals").add("Java");
        output.putArray("negativeSignals");
        output.putArray("attentionPoints");
        output.putArray("matchedPreferences").add("成都");
        output.putArray("unmatchedPreferences");
        return output;
    }

    private void assertInvalid(ObjectNode output, String messagePart) {
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(AnalysisDefinitionValidationException.class)
                .hasMessageContaining(messagePart);
    }
}
