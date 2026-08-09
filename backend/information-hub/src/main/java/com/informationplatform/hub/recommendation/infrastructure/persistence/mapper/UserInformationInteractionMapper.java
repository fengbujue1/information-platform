package com.informationplatform.hub.recommendation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.UserInformationInteractionPo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 提供用户 Information 当前交互聚合状态的基础持久化能力。 */
@Mapper
public interface UserInformationInteractionMapper extends BaseMapper<UserInformationInteractionPo> {

    /** 原子累计查看次数；并发首次写入由唯一键收敛为同一 current aggregate。 */
    @Insert("""
            INSERT INTO user_information_interaction (
                user_id, information_id, view_count, last_viewed_at,
                feedback_state, last_recommendation_item_id
            ) VALUES (
                #{userId}, #{informationId}, 1, #{occurredAt},
                'NONE', #{recommendationItemId}
            )
            ON DUPLICATE KEY UPDATE
                view_count = view_count + 1,
                last_viewed_at = #{occurredAt},
                last_recommendation_item_id = COALESCE(
                    #{recommendationItemId}, last_recommendation_item_id)
            """)
    int recordView(
            @Param("userId") long userId,
            @Param("informationId") long informationId,
            @Param("recommendationItemId") Long recommendationItemId,
            @Param("occurredAt") LocalDateTime occurredAt);

    /** 原子创建或覆盖通用 Feedback，不改变 view 聚合与 JOB disposition。 */
    @Insert("""
            INSERT INTO user_information_interaction (
                user_id, information_id, view_count,
                feedback_state, feedback_updated_at, last_recommendation_item_id
            ) VALUES (
                #{userId}, #{informationId}, 0,
                #{feedbackState}, #{occurredAt}, #{recommendationItemId}
            )
            ON DUPLICATE KEY UPDATE
                feedback_state = #{feedbackState},
                feedback_updated_at = #{occurredAt},
                last_recommendation_item_id = COALESCE(
                    #{recommendationItemId}, last_recommendation_item_id)
            """)
    int replaceFeedback(
            @Param("userId") long userId,
            @Param("informationId") long informationId,
            @Param("feedbackState") String feedbackState,
            @Param("recommendationItemId") Long recommendationItemId,
            @Param("occurredAt") LocalDateTime occurredAt);

    /** 确保 Interaction Core 存在；并发创建依赖唯一键安全合并。 */
    @Insert("""
            INSERT INTO user_information_interaction (
                user_id, information_id, view_count, feedback_state,
                last_recommendation_item_id
            ) VALUES (
                #{userId}, #{informationId}, 0, 'NONE', #{recommendationItemId}
            )
            ON DUPLICATE KEY UPDATE
                last_recommendation_item_id = COALESCE(
                    #{recommendationItemId}, last_recommendation_item_id)
            """)
    int ensureInteraction(
            @Param("userId") long userId,
            @Param("informationId") long informationId,
            @Param("recommendationItemId") Long recommendationItemId);

    /** 按 Owner 与 Information 读取唯一 current aggregate。 */
    @Select("""
            SELECT id, user_id, information_id, view_count, last_viewed_at,
                   feedback_state, feedback_updated_at, last_recommendation_item_id,
                   created_at, updated_at
            FROM user_information_interaction
            WHERE user_id = #{userId}
              AND information_id = #{informationId}
            """)
    UserInformationInteractionPo selectOwnedInteraction(
            @Param("userId") long userId,
            @Param("informationId") long informationId);
}
