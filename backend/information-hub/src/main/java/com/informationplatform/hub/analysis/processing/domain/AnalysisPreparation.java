package com.informationplatform.hub.analysis.processing.domain;

/** 单条调用准备结果：可能直接复用成功结果，也可能需要执行新 Invocation。 */
public record AnalysisPreparation(
        /** 已成功复用时返回查询视图，否则为空。 */ InformationAnalysisView reused,
        /** 需要真实调用时返回执行计划，否则为空。 */ AnalysisExecutionPlan executionPlan) {

    public AnalysisPreparation {
        if ((reused == null) == (executionPlan == null)) {
            throw new IllegalArgumentException(
                    "Analysis preparation must contain exactly one outcome");
        }
    }

    public static AnalysisPreparation reused(InformationAnalysisView view) {
        return new AnalysisPreparation(view, null);
    }

    public static AnalysisPreparation execute(AnalysisExecutionPlan plan) {
        return new AnalysisPreparation(null, plan);
    }
}
