package com.informationplatform.hub.analysis.processing.domain;

import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionId;
import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;
import com.informationplatform.hub.analysis.definition.domain.AnalysisPurpose;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;

/** 一次可复现 Prompt Assembly 的版本化执行上下文。 */
public record AssembledAnalysisPrompt(
        /** Analysis Definition 的稳定 Key 与版本。 */
        AnalysisDefinitionId definitionId,
        /** 本次分析适用的信息类型。 */
        AnalysisInformationType informationType,
        /** 本次分析的业务目的。 */
        AnalysisPurpose analysisPurpose,
        /** 平台 System Prompt 资源版本。 */
        int systemPromptVersion,
        /** 平台 Output Schema 资源版本。 */
        int outputSchemaVersion,
        /** User Prompt 所属 Profile 主键。 */
        long promptProfileId,
        /** 不可变 User Prompt Version 主键。 */
        long promptVersionId,
        /** Profile 内 User Prompt 版本号。 */
        int promptVersionNo,
        /** User Prompt 原文的稳定内容摘要。 */
        String promptContentHash,
        /** 本次输入绑定的不可变 Snapshot 主键。 */
        long snapshotId,
        /** Snapshot 所属 Information 主键。 */
        long informationId,
        /** 已按固定角色和顺序组装的 Provider 请求。 */
        AiProviderRequest providerRequest) {

    public AssembledAnalysisPrompt {
        if (definitionId == null
                || informationType == null
                || analysisPurpose == null
                || providerRequest == null) {
            throw new IllegalArgumentException("Analysis prompt metadata must not be null");
        }
        if (systemPromptVersion <= 0
                || outputSchemaVersion <= 0
                || promptProfileId <= 0
                || promptVersionId <= 0
                || promptVersionNo <= 0
                || snapshotId <= 0
                || informationId <= 0) {
            throw new IllegalArgumentException("Analysis prompt version metadata must be positive");
        }
        if (promptContentHash == null || promptContentHash.isBlank()) {
            throw new IllegalArgumentException("Prompt content hash must not be blank");
        }
    }
}
