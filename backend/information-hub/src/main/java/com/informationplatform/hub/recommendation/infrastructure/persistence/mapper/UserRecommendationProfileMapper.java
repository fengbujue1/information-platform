package com.informationplatform.hub.recommendation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserRecommendationProfilePo;
import org.apache.ibatis.annotations.Mapper;

/** 提供 Recommendation Profile 的基础持久化能力。 */
@Mapper
public interface UserRecommendationProfileMapper extends BaseMapper<UserRecommendationProfilePo> {
}
