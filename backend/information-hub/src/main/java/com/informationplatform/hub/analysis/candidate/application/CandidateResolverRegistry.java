package com.informationplatform.hub.analysis.candidate.application;

import com.informationplatform.hub.analysis.candidate.domain.CandidateResolver;
import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 按 Information 类型索引唯一 CandidateResolver。 */
@Component
public class CandidateResolverRegistry {

    /** 启动时冻结的 Resolver 集合。 */
    private final Map<AnalysisInformationType, CandidateResolver> resolvers;

    public CandidateResolverRegistry(List<CandidateResolver> resolvers) {
        Map<AnalysisInformationType, CandidateResolver> indexed =
                new EnumMap<>(AnalysisInformationType.class);
        for (CandidateResolver resolver : resolvers) {
            CandidateResolver duplicate = indexed.putIfAbsent(resolver.informationType(), resolver);
            if (duplicate != null) {
                throw new IllegalStateException(
                        "Duplicate Candidate Resolver: " + resolver.informationType());
            }
        }
        this.resolvers = Map.copyOf(indexed);
    }

    /** 返回指定类型的 Resolver；未实现的类型立即失败。 */
    public CandidateResolver require(AnalysisInformationType informationType) {
        CandidateResolver resolver = resolvers.get(informationType);
        if (resolver == null) {
            throw new IllegalArgumentException(
                    "Candidate Resolver is unavailable: " + informationType);
        }
        return resolver;
    }
}
