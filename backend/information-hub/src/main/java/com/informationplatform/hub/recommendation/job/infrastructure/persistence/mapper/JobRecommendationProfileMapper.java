package com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.JobRecommendationProfilePo;
import org.apache.ibatis.annotations.Mapper;

/** 提供 JOB Recommendation Profile 扩展的基础持久化能力。 */
@Mapper
public interface JobRecommendationProfileMapper extends BaseMapper<JobRecommendationProfilePo> {
}
