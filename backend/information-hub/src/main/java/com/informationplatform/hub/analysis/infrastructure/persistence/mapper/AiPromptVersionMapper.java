package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供不可变 Prompt 版本的基础持久化能力。 */
@Mapper
public interface AiPromptVersionMapper extends BaseMapper<AiPromptVersionPo> {
}
