package com.informationplatform.hub.information.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationSnapshotPo;
import org.apache.ibatis.annotations.Mapper;

/** 提供信息版本快照表的基础持久化操作。 */
@Mapper
public interface InformationSnapshotMapper extends BaseMapper<InformationSnapshotPo> {}
