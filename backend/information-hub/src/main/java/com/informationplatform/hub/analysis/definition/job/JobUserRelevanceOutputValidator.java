package com.informationplatform.hub.analysis.definition.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionValidationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class JobUserRelevanceOutputValidator {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "schemaVersion",
            "relevanceScore",
            "confidence",
            "summary",
            "positiveSignals",
            "negativeSignals",
            "attentionPoints",
            "matchedPreferences",
            "unmatchedPreferences");

    /**
     * 按 JOB_USER_RELEVANCE_V1 冻结契约严格校验并转换模型输出。
     *
     * <p>所有字段均为必填且拒绝未知字段，避免模型输出被宽松反序列化后静默改变语义。
     */
    public JobUserRelevanceOutput validate(JsonNode output) {
        if (output == null || !output.isObject()) {
            throw invalid("Output must be a JSON object");
        }
        Set<String> actualFields = new HashSet<>();
        output.fieldNames().forEachRemaining(actualFields::add);
        if (!actualFields.equals(REQUIRED_FIELDS)) {
            throw invalid("Output fields must exactly match schema version 1");
        }

        int schemaVersion = requiredInteger(output, "schemaVersion");
        if (schemaVersion != 1) {
            throw invalid("schemaVersion must be 1");
        }
        int relevanceScore = requiredInteger(output, "relevanceScore");
        if (relevanceScore < 0 || relevanceScore > 100) {
            throw invalid("relevanceScore must be between 0 and 100");
        }
        BigDecimal confidence = requiredNumber(output, "confidence");
        if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
            throw invalid("confidence must be between 0 and 1");
        }

        String summary = requiredText(output, "summary", 1000);
        return new JobUserRelevanceOutput(
                schemaVersion,
                relevanceScore,
                confidence,
                summary,
                requiredTextArray(output, "positiveSignals"),
                requiredTextArray(output, "negativeSignals"),
                requiredTextArray(output, "attentionPoints"),
                requiredTextArray(output, "matchedPreferences"),
                requiredTextArray(output, "unmatchedPreferences"));
    }

    private int requiredInteger(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw invalid(field + " must be an integer");
        }
        return value.intValue();
    }

    private BigDecimal requiredNumber(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isNumber()) {
            throw invalid(field + " must be a number");
        }
        return value.decimalValue();
    }

    private String requiredText(JsonNode object, String field, int maxCodePoints) {
        JsonNode value = object.get(field);
        if (value == null || !value.isTextual()) {
            throw invalid(field + " must be a string");
        }
        String text = value.textValue();
        if (text.codePointCount(0, text.length()) > maxCodePoints) {
            throw invalid(field + " exceeds " + maxCodePoints + " Unicode characters");
        }
        return text;
    }

    private List<String> requiredTextArray(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isArray()) {
            throw invalid(field + " must be an array");
        }
        if (value.size() > 20) {
            throw invalid(field + " must contain at most 20 items");
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                throw invalid(field + " must contain only strings");
            }
            String text = item.textValue();
            if (text.codePointCount(0, text.length()) > 500) {
                throw invalid(field + " items must not exceed 500 Unicode characters");
            }
            result.add(text);
        }
        return result;
    }

    private AnalysisDefinitionValidationException invalid(String message) {
        return new AnalysisDefinitionValidationException("ANALYSIS_OUTPUT_INVALID", message);
    }
}
