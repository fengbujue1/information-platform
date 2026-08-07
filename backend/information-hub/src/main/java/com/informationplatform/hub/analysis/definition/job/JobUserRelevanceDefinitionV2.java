package com.informationplatform.hub.analysis.definition.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionId;
import org.springframework.stereotype.Component;

/**
 * JOB 用户相关性分析 V2。
 *
 * <p>V2 继续复用 V1 的输入投影、System Prompt、Output Schema 和输出校验规则，
 * 仅将单次模型输出 Token 上限提高到 5000。
 */
@Component
public class JobUserRelevanceDefinitionV2 extends JobUserRelevanceDefinition {

    /** V2 Definition 版本。 */
    public static final int VERSION = 2;

    /** V2 单次允许的最大输出 Token。 */
    private static final int MAX_OUTPUT_TOKENS = 5000;

    public JobUserRelevanceDefinitionV2(
        ObjectMapper objectMapper,
        JobUserRelevanceInputProjector inputProjector,
        JobUserRelevanceOutputValidator outputValidator) {
        super(objectMapper, inputProjector, outputValidator);
    }

    @Override
    public AnalysisDefinitionId id() {
        return new AnalysisDefinitionId(KEY, VERSION);
    }

    @Override
    public int maxOutputTokens() {
        return MAX_OUTPUT_TOKENS;
    }
}
