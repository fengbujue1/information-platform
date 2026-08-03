package com.informationplatform.hub.analysis.processing.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.informationplatform.hub.analysis.provider.domain.AiProviderResult;

/** 已通过单一 JSON 解析和 Definition Schema 校验的 Provider 输出。 */
public record ValidatedAnalysisOutput<O>(
        /** 可安全写入 result_json 的已验证 JSON。 */
        JsonNode resultJson,
        /** Definition 转换后的类型化业务结果。 */
        O value,
        /** 原始 Provider 元数据与 Actual Usage。 */
        AiProviderResult providerResult) {

    public ValidatedAnalysisOutput {
        if (resultJson == null || value == null || providerResult == null) {
            throw new IllegalArgumentException("Validated analysis output must not be null");
        }
        resultJson = resultJson.deepCopy();
    }

    @Override
    public JsonNode resultJson() {
        return resultJson.deepCopy();
    }
}
