package com.informationplatform.hub.recommendation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供预计算 Recommendation Run 的基础持久化能力。 */
@Mapper
public interface RecommendationRunMapper extends BaseMapper<RecommendationRunPo> {
}
