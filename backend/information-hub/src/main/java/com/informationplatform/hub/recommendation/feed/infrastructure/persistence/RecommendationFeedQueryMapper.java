package com.informationplatform.hub.recommendation.feed.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 在已选定成功 Run 上应用 current Interaction visibility 的 Feed 查询。 */
@Mapper
public interface RecommendationFeedQueryMapper {

    /** 统计 hard exclusion 生效后的当前可见 Item 数。 */
    @Select("""
            SELECT COUNT(*)
            FROM recommendation_item item
            LEFT JOIN user_information_interaction interaction
              ON interaction.user_id = #{userId}
             AND interaction.information_id = item.information_id
            LEFT JOIN user_job_disposition disposition
              ON disposition.interaction_id = interaction.id
            WHERE item.run_id = #{runId}
              AND COALESCE(interaction.feedback_state, 'NONE') <> 'NOT_INTERESTED'
              AND COALESCE(disposition.job_disposition, 'NONE') <> 'CONTACTED_NOT_SUITABLE'
            """)
    long countVisible(@Param("runId") long runId, @Param("userId") long userId);

    /** 按冻结 rank 分页读取可见 Item，并投影 current Interaction 与当前 JOB 展示字段。 */
    @Select("""
            SELECT item.id AS recommendation_item_id,
                   item.information_id,
                   item.snapshot_id,
                   item.rank_no,
                   item.final_score,
                   item.ai_relevance_score,
                   item.profile_match_score,
                   item.freshness_score,
                   item.reasons_json,
                   COALESCE(interaction.feedback_state, 'NONE') AS feedback_state,
                   COALESCE(disposition.job_disposition, 'NONE') AS job_disposition,
                   COALESCE(interaction.view_count, 0) AS view_count,
                   information.title,
                   job.company_name,
                   job.salary_text,
                   job.location_name,
                   job.remote_type,
                   information.source_url
            FROM recommendation_item item
            INNER JOIN information_item information
              ON information.id = item.information_id
            INNER JOIN job_information job
              ON job.information_id = item.information_id
            LEFT JOIN user_information_interaction interaction
              ON interaction.user_id = #{userId}
             AND interaction.information_id = item.information_id
            LEFT JOIN user_job_disposition disposition
              ON disposition.interaction_id = interaction.id
            WHERE item.run_id = #{runId}
              AND COALESCE(interaction.feedback_state, 'NONE') <> 'NOT_INTERESTED'
              AND COALESCE(disposition.job_disposition, 'NONE') <> 'CONTACTED_NOT_SUITABLE'
            ORDER BY item.rank_no ASC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<RecommendationFeedQueryRow> selectVisible(
            @Param("runId") long runId,
            @Param("userId") long userId,
            @Param("limit") int limit,
            @Param("offset") long offset);
}
