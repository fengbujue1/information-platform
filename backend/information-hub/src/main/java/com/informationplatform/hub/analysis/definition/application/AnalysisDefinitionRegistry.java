package com.informationplatform.hub.analysis.definition.application;

import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinitionId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AnalysisDefinitionRegistry {

    /** 按稳定 key/version 索引的不可变 Definition 集合。 */
    private final Map<AnalysisDefinitionId, AnalysisDefinition<?, ?>> definitions;

    public AnalysisDefinitionRegistry(List<AnalysisDefinition<?, ?>> definitions) {
        Map<AnalysisDefinitionId, AnalysisDefinition<?, ?>> indexed = new LinkedHashMap<>();
        for (AnalysisDefinition<?, ?> definition : definitions) {
            // 启动时拒绝重复注册，防止同一持久化标识在运行期产生歧义。
            AnalysisDefinition<?, ?> duplicate = indexed.putIfAbsent(definition.id(), definition);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate Analysis Definition: " + definition.id());
            }
        }
        this.definitions = Collections.unmodifiableMap(indexed);
    }

    /** 按持久化 key/version 获取 Definition，未知版本立即失败。 */
    public AnalysisDefinition<?, ?> require(String key, int version) {
        AnalysisDefinitionId id = new AnalysisDefinitionId(key, version);
        AnalysisDefinition<?, ?> definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown Analysis Definition: " + id);
        }
        return definition;
    }

    public List<AnalysisDefinition<?, ?>> all() {
        return List.copyOf(definitions.values());
    }
}
