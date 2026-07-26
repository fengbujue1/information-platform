package com.informationplatform.hub.information.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.informationplatform.hub.information.infrastructure.persistence.po.InformationItemPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InformationItemMapper extends BaseMapper<InformationItemPo> {

    /**
     * 按幂等键查询当前信息并加行锁。
     *
     * <p>调用方必须处于事务中，以串行化同一来源信息的合并和版本更新。
     */
    default InformationItemPo selectByIdentityForUpdate(
            String source, String informationType, String sourceItemId) {
        // 幂等键字段均使用参数绑定，FOR UPDATE 是固定 SQL 片段。
        return selectOne(Wrappers.<InformationItemPo>lambdaQuery()
                .eq(InformationItemPo::getSource, source)
                .eq(InformationItemPo::getInformationType, informationType)
                .eq(InformationItemPo::getSourceItemId, sourceItemId)
                .last("FOR UPDATE"));
    }
}
