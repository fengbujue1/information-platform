package com.informationplatform.hub.recommendation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserInformationInteractionPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供用户 Information 当前交互聚合状态的基础持久化能力。 */
@Mapper
public interface UserInformationInteractionMapper extends BaseMapper<UserInformationInteractionPo> {
}
