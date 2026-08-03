package com.informationplatform.hub.analysis.definition.domain;

public record AnalysisDefinitionId(
        /** 持久化使用的稳定 Definition Key。 */
        String key,
        /** Definition 的正整数版本号。 */
        int version) {

    public AnalysisDefinitionId {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Analysis Definition key must not be blank");
        }
        if (version <= 0) {
            throw new IllegalArgumentException("Analysis Definition version must be positive");
        }
    }
}
