package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiPromptProfilePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 提供 Prompt 配置档案的基础持久化能力。 */
@Mapper
public interface AiPromptProfileMapper extends BaseMapper<AiPromptProfilePo> {

    /** 按 Owner 锁定 Profile，串行化 Version 编号和 Active Version 切换。 */
    @Select("""
            SELECT id,
                   user_id,
                   name,
                   analysis_definition_key,
                   active_version_id,
                   status,
                   created_at,
                   updated_at
            FROM ai_prompt_profile
            WHERE id = #{profileId}
              AND user_id = #{userId}
            FOR UPDATE
            """)
    AiPromptProfilePo selectOwnedByIdForUpdate(
            @Param("profileId") long profileId,
            @Param("userId") long userId);

    /** 在已锁定并校验归属后更新 Active Version 指针。 */
    @Update("""
            UPDATE ai_prompt_profile
            SET active_version_id = #{versionId}
            WHERE id = #{profileId}
              AND user_id = #{userId}
            """)
    int updateActiveVersion(
            @Param("profileId") long profileId,
            @Param("userId") long userId,
            @Param("versionId") long versionId);

    /** 在已锁定并校验归属后只更新状态，使数据库维护 updated_at。 */
    @Update("""
            UPDATE ai_prompt_profile
            SET status = #{status}
            WHERE id = #{profileId}
              AND user_id = #{userId}
            """)
    int updateStatus(
            @Param("profileId") long profileId,
            @Param("userId") long userId,
            @Param("status") String status);
}
