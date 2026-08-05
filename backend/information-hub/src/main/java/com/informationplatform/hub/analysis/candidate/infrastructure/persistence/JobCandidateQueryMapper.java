package com.informationplatform.hub.analysis.candidate.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 使用 FIRST_INGESTED 和当前 Snapshot 读取 JOB 候选。 */
@Mapper
public interface JobCandidateQueryMapper {

    /** 统计窗口总数与当前逻辑身份已成功数量。 */
    @Select("""
            SELECT COUNT(*) AS total_in_window,
                   COALESCE(SUM(CASE WHEN EXISTS (
                       SELECT 1
                       FROM information_analysis analysis
                       WHERE analysis.user_id = #{userId}
                         AND analysis.snapshot_id = snapshot.id
                         AND analysis.prompt_version_id = #{promptVersionId}
                         AND analysis.analysis_definition_key = #{definitionKey}
                         AND analysis.analysis_definition_version = #{definitionVersion}
                         AND analysis.status = 'SUCCEEDED'
                   ) THEN 1 ELSE 0 END), 0) AS already_analyzed_count
            FROM information_item item
            JOIN information_snapshot snapshot
              ON snapshot.information_id = item.id
             AND snapshot.version_no = item.current_version_no
            WHERE item.information_type = 'JOB'
              AND item.first_seen_time >= #{windowStart}
              AND item.first_seen_time < #{windowEnd}
            """)
    CandidateWindowCountsRow countWindow(
            @Param("userId") long userId,
            @Param("promptVersionId") long promptVersionId,
            @Param("definitionKey") String definitionKey,
            @Param("definitionVersion") int definitionVersion,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);

    /** 返回前 limit 个尚未成功的当前 Snapshot，顺序固定为 first_seen_time/id 倒序。 */
    @Select("""
            SELECT item.id AS information_id,
                   snapshot.id AS snapshot_id,
                   item.information_type,
                   item.first_seen_time,
                   snapshot.title,
                   snapshot.content,
                   snapshot.standardized_payload
            FROM information_item item
            JOIN information_snapshot snapshot
              ON snapshot.information_id = item.id
             AND snapshot.version_no = item.current_version_no
            WHERE item.information_type = 'JOB'
              AND item.first_seen_time >= #{windowStart}
              AND item.first_seen_time < #{windowEnd}
              AND NOT EXISTS (
                  SELECT 1
                  FROM information_analysis analysis
                  WHERE analysis.user_id = #{userId}
                    AND analysis.snapshot_id = snapshot.id
                    AND analysis.prompt_version_id = #{promptVersionId}
                    AND analysis.analysis_definition_key = #{definitionKey}
                    AND analysis.analysis_definition_version = #{definitionVersion}
                    AND analysis.status = 'SUCCEEDED'
              )
            ORDER BY item.first_seen_time DESC, item.id DESC
            LIMIT #{limit}
            """)
    List<JobCandidateRow> selectEligible(
            @Param("userId") long userId,
            @Param("promptVersionId") long promptVersionId,
            @Param("definitionKey") String definitionKey,
            @Param("definitionVersion") int definitionVersion,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd,
            @Param("limit") int limit);
}
