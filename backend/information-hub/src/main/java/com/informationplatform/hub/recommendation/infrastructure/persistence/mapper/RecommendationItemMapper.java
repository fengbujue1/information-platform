package com.informationplatform.hub.recommendation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationItemPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供不可变 Recommendation Item 历史事实的基础持久化能力。 */
@Mapper
public interface RecommendationItemMapper extends BaseMapper<RecommendationItemPo> {
}
