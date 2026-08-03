package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisSchedulePo;
import org.apache.ibatis.annotations.Mapper;

/** 提供每日分析计划的基础持久化能力。 */
@Mapper
public interface AiAnalysisScheduleMapper extends BaseMapper<AiAnalysisSchedulePo> {
}
