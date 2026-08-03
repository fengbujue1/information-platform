package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiModelInvocationPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供模型调用及 Provider 实际用量记录的基础持久化能力。 */
@Mapper
public interface AiModelInvocationMapper extends BaseMapper<AiModelInvocationPo> {
}
