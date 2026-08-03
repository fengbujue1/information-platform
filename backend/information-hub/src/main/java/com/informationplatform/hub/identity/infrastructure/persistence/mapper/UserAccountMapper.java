package com.informationplatform.hub.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.identity.infrastructure.persistence.po.UserAccountPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供最小用户账号表的基础持久化操作。 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountPo> {}
