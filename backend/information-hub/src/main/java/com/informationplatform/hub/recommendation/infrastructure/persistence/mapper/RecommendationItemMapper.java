package com.informationplatform.hub.recommendation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationItemPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 提供不可变 Recommendation Item 历史事实的基础持久化能力。 */
@Mapper
public interface RecommendationItemMapper extends BaseMapper<RecommendationItemPo> {

    /** 校验归因 Item 同时属于当前 Owner 且指向目标 Information。 */
    @Select("""
            SELECT COUNT(*)
            FROM recommendation_item item
            INNER JOIN recommendation_run run ON run.id = item.run_id
            WHERE item.id = #{itemId}
              AND item.information_id = #{informationId}
              AND run.user_id = #{userId}
            """)
    long countOwnedAttribution(
            @Param("itemId") long itemId,
            @Param("informationId") long informationId,
            @Param("userId") long userId);
}
