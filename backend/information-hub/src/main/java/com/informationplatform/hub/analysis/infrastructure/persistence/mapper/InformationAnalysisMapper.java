package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.InformationAnalysisPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供快照级逻辑分析结果的基础持久化能力。 */
@Mapper
public interface InformationAnalysisMapper extends BaseMapper<InformationAnalysisPo> {
}
