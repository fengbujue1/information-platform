package com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.UserJobDispositionPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供 JOB disposition 扩展的基础持久化能力。 */
@Mapper
public interface UserJobDispositionMapper extends BaseMapper<UserJobDispositionPo> {
}
