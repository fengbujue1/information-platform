package com.informationplatform.hub.analysis.definition.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionValidationException;
import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;
import com.informationplatform.hub.analysis.definition.domain.AnalysisSnapshotSource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JobUserRelevanceInputProjector {

    /**
     * 从不可变 Snapshot 投影冻结的 13 个输入字段。
     *
     * <p>标题和正文取快照独立列，其余字段只取 standardizedPayload.job；缺失值保持 null，
     * rawPayload 不在输入类型中，因此不会被透传给模型。
     */
    public JobUserRelevanceInput project(AnalysisSnapshotSource source) {
        if (source.informationType() != AnalysisInformationType.JOB) {
            throw invalid("Snapshot informationType must be JOB");
        }
        JsonNode root = source.standardizedPayload();
        if (!root.isObject()) {
            throw invalid("standardizedPayload must be an object");
        }
        JsonNode job = root.get("job");
        if (job == null || !job.isObject()) {
            throw invalid("standardizedPayload.job must be an object");
        }
        return new JobUserRelevanceInput(
                source.title(),
                source.content(),
                nullableText(job, "companyName"),
                nullableText(job, "salaryText"),
                nullableText(job, "locationName"),
                nullableText(job, "cityName"),
                nullableText(job, "experienceText"),
                nullableText(job, "educationText"),
                nullableText(job, "remoteType"),
                nullableText(job, "jobStatus"),
                nullableTextArray(job, "sourceTags"),
                nullableTextArray(job, "sourceSkillTags"),
                nullableTextArray(job, "welfare"));
    }

    private String nullableText(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw invalid(field + " must be a string or null");
        }
        return value.textValue();
    }

    private List<String> nullableTextArray(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isArray()) {
            throw invalid(field + " must be an array of strings or null");
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                throw invalid(field + " must contain only strings");
            }
            result.add(item.textValue());
        }
        return result;
    }

    private AnalysisDefinitionValidationException invalid(String message) {
        return new AnalysisDefinitionValidationException("ANALYSIS_INPUT_INVALID", message);
    }
}
