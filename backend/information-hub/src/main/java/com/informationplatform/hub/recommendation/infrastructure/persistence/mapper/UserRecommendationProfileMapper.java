package com.informationplatform.hub.recommendation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserRecommendationProfilePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 提供 Recommendation Profile 的基础持久化能力。 */
@Mapper
public interface UserRecommendationProfileMapper extends BaseMapper<UserRecommendationProfilePo> {

    /** 按 Owner 与 Information Type 读取唯一 Profile Core。 */
    @Select("""
            SELECT id,
                   user_id,
                   information_type,
                   analysis_prompt_profile_id,
                   window_days,
                   top_n,
                   content_hash,
                   created_at,
                   updated_at
            FROM user_recommendation_profile
            WHERE user_id = #{userId}
              AND information_type = #{informationType}
            """)
    UserRecommendationProfilePo selectOwnedByType(
            @Param("userId") long userId,
            @Param("informationType") String informationType);

    /** 锁定当前 Owner 与 Information Type 的 Profile Core，串行化完整替换。 */
    @Select("""
            SELECT id,
                   user_id,
                   information_type,
                   analysis_prompt_profile_id,
                   window_days,
                   top_n,
                   content_hash,
                   created_at,
                   updated_at
            FROM user_recommendation_profile
            WHERE user_id = #{userId}
              AND information_type = #{informationType}
            FOR UPDATE
            """)
    UserRecommendationProfilePo selectOwnedByTypeForUpdate(
            @Param("userId") long userId,
            @Param("informationType") String informationType);

    /** 完整替换已锁定 Core 的可编辑字段，并让数据库维护 updated_at。 */
    @Update("""
            UPDATE user_recommendation_profile
            SET analysis_prompt_profile_id = #{analysisPromptProfileId},
                window_days = #{windowDays},
                top_n = #{topN},
                content_hash = #{contentHash}
            WHERE id = #{id}
              AND user_id = #{userId}
              AND information_type = #{informationType}
            """)
    int updateOwnedProfile(
            @Param("id") long id,
            @Param("userId") long userId,
            @Param("informationType") String informationType,
            @Param("analysisPromptProfileId") long analysisPromptProfileId,
            @Param("windowDays") int windowDays,
            @Param("topN") int topN,
            @Param("contentHash") String contentHash);
}
