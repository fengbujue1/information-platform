package com.informationplatform.hub.analysis.processing.domain;

import com.informationplatform.hub.analysis.definition.domain.AnalysisDefinition;
import com.informationplatform.hub.analysis.provider.domain.AiProviderRequest;

/** 已在短事务内冻结并持久化、可在事务外执行的单条 Analysis 计划。 */
public record AnalysisExecutionPlan(
        /** 新建或复用的 Analysis 主键。 */ long analysisId,
        /** 本次 RUNNING Invocation 主键。 */ long invocationId,
        /** 当前 Owner 主键。 */ long userId,
        /** 冻结的 Analysis Definition。 */ AnalysisDefinition<?, ?> definition,
        /** 冻结的 Provider 请求。 */ AiProviderRequest providerRequest) {

    public AnalysisExecutionPlan {
        if (analysisId <= 0
                || invocationId <= 0
                || userId <= 0
                || definition == null
                || providerRequest == null) {
            throw new IllegalArgumentException("Analysis execution plan is invalid");
        }
    }
}
