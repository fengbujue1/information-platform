package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import org.apache.ibatis.annotations.Mapper;

/** 提供 Prompt 配置档案的基础持久化能力。 */
@Mapper
public interface AiPromptProfileMapper extends BaseMapper<AiPromptProfilePo> {
}
