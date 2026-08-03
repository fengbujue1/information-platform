package com.informationplatform.hub.analysis.definition.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionId;
import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;
import com.informationplatform.hub.analysis.definition.domain.AnalysisPurpose;
import com.informationplatform.hub.analysis.definition.domain.AnalysisSnapshotSource;
import com.informationplatform.hub.analysis.definition.domain.SourceContentBoundary;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class JobUserRelevanceDefinition
        implements AnalysisDefinition<JobUserRelevanceInput, JobUserRelevanceOutput> {

    public static final String KEY = "JOB_USER_RELEVANCE";
    public static final int VERSION = 1;
    private static final int SYSTEM_PROMPT_VERSION = 1;
    private static final int OUTPUT_SCHEMA_VERSION = 1;
    private static final int MAX_OUTPUT_TOKENS = 1000;
    private static final String SYSTEM_PROMPT_RESOURCE =
            "ai/definitions/job-user-relevance-v1-system-prompt-v1.txt";
    private static final String OUTPUT_SCHEMA_RESOURCE =
            "ai/definitions/job-user-relevance-v1-output-schema-v1.json";
    private static final SourceContentBoundary SOURCE_BOUNDARY =
            new SourceContentBoundary("<UNTRUSTED_SOURCE_DATA>", "</UNTRUSTED_SOURCE_DATA>");

    /** 用于序列化冻结输入和读取 JSON Schema。 */
    private final ObjectMapper objectMapper;
    /** JOB 快照输入投影器。 */
    private final JobUserRelevanceInputProjector inputProjector;
    /** JOB 用户相关性输出校验器。 */
    private final JobUserRelevanceOutputValidator outputValidator;
    /** 启动时加载的平台 System Prompt。 */
    private final String systemPrompt;
    /** 启动时加载的平台 Output Schema。 */
    private final JsonNode outputSchema;

    public JobUserRelevanceDefinition(
            ObjectMapper objectMapper,
            JobUserRelevanceInputProjector inputProjector,
            JobUserRelevanceOutputValidator outputValidator) {
        this.objectMapper = objectMapper;
        this.inputProjector = inputProjector;
        this.outputValidator = outputValidator;
        this.systemPrompt = readTextResource(SYSTEM_PROMPT_RESOURCE);
        this.outputSchema = readJsonResource(OUTPUT_SCHEMA_RESOURCE);
    }

    @Override
    public AnalysisDefinitionId id() {
        return new AnalysisDefinitionId(KEY, VERSION);
    }

    @Override
    public AnalysisInformationType informationType() {
        return AnalysisInformationType.JOB;
    }

    @Override
    public AnalysisPurpose analysisPurpose() {
        return AnalysisPurpose.USER_RELEVANCE;
    }

    @Override
    public int systemPromptVersion() {
        return SYSTEM_PROMPT_VERSION;
    }

    @Override
    public String systemPrompt() {
        return systemPrompt;
    }

    @Override
    public int outputSchemaVersion() {
        return OUTPUT_SCHEMA_VERSION;
    }

    @Override
    public JsonNode outputSchema() {
        return outputSchema.deepCopy();
    }

    @Override
    public SourceContentBoundary sourceContentBoundary() {
        return SOURCE_BOUNDARY;
    }

    @Override
    public int maxOutputTokens() {
        return MAX_OUTPUT_TOKENS;
    }

    @Override
    public JobUserRelevanceInput projectInput(AnalysisSnapshotSource source) {
        return inputProjector.project(source);
    }

    @Override
    public String renderUserMessage(JobUserRelevanceInput input) {
        try {
            // JSON 序列化会转义来源中的换行，平台边界标记始终由 Definition 自己添加。
            return SOURCE_BOUNDARY.wrap(objectMapper.writeValueAsString(input));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Analysis input could not be serialized", exception);
        }
    }

    @Override
    public JobUserRelevanceOutput validateOutput(JsonNode output) {
        return outputValidator.validate(output);
    }

    private String readTextResource(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Analysis resource could not be loaded: " + path, exception);
        }
    }

    private JsonNode readJsonResource(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readTree(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Analysis resource could not be loaded: " + path, exception);
        }
    }
}
