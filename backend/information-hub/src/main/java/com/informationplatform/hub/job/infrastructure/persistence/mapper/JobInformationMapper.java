package com.informationplatform.hub.job.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.job.infrastructure.persistence.po.JobInformationPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供职位扩展表的基础持久化操作。 */
@Mapper
public interface JobInformationMapper extends BaseMapper<JobInformationPo> {}
