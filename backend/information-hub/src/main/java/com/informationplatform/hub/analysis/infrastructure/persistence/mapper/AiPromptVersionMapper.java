package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptVersionPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 提供不可变 Prompt 版本的基础持久化能力。 */
@Mapper
public interface AiPromptVersionMapper extends BaseMapper<AiPromptVersionPo> {

    /** 返回指定 Profile 当前最大版本号；尚无 Version 时返回 0。 */
    @Select("""
            SELECT COALESCE(MAX(version_no), 0)
            FROM ai_prompt_version
            WHERE prompt_profile_id = #{profileId}
            """)
    int selectMaxVersionNo(@Param("profileId") long profileId);
}
