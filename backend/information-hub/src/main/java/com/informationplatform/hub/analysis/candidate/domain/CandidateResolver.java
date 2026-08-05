package com.informationplatform.hub.analysis.candidate.domain;

import com.informationplatform.hub.analysis.definition.domain.AnalysisInformationType;

/** 面向不同 Information 类型的候选解析扩展点。 */
public interface CandidateResolver {

    /** 本 Resolver 支持的信息类型。 */
    AnalysisInformationType informationType();

    /** 按固定绝对窗口返回统计与稳定排序候选。 */
    CandidateResolution resolve(CandidateResolutionRequest request);
}
