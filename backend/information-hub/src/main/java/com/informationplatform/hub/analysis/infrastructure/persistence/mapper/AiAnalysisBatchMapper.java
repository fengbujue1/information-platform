package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供手动与定时共用分析批次的基础持久化能力。 */
@Mapper
public interface AiAnalysisBatchMapper extends BaseMapper<AiAnalysisBatchPo> {
}
