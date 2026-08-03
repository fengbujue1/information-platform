package com.informationplatform.hub.analysis.definition.domain;

import com.fasterxml.jackson.databind.JsonNode;

/** 冻结某类分析的输入、平台提示词和输出契约。 */
public interface AnalysisDefinition<I, O> {

    AnalysisDefinitionId id();

    AnalysisInformationType informationType();

    AnalysisPurpose analysisPurpose();

    int systemPromptVersion();

    String systemPrompt();

    int outputSchemaVersion();

    JsonNode outputSchema();

    SourceContentBoundary sourceContentBoundary();

    int maxOutputTokens();

    I projectInput(AnalysisSnapshotSource source);

    String renderUserMessage(I input);

    O validateOutput(JsonNode output);
}
