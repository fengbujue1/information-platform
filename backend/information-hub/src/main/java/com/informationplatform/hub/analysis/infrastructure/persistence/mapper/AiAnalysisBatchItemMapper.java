package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchItemPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供冻结批次候选明细的基础持久化能力。 */
@Mapper
public interface AiAnalysisBatchItemMapper extends BaseMapper<AiAnalysisBatchItemPo> {
}
