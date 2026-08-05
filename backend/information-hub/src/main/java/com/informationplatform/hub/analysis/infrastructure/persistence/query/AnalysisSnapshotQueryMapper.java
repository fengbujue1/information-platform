package com.informationplatform.hub.analysis.infrastructure.persistence.query;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 按 Information 与 Snapshot 身份读取安全 AI 投影。 */
@Mapper
public interface AnalysisSnapshotQueryMapper {

    /** 读取指定 Information 的当前不可变 Snapshot，不返回 raw_payload。 */
    @Select("""
            SELECT snapshot.id AS snapshot_id,
                   item.id AS information_id,
                   item.information_type,
                   snapshot.title,
                   snapshot.content,
                   snapshot.standardized_payload
            FROM information_item item
            JOIN information_snapshot snapshot
              ON snapshot.information_id = item.id
             AND snapshot.version_no = item.current_version_no
            WHERE item.id = #{informationId}
            """)
    AnalysisSnapshotRow selectCurrent(@Param("informationId") long informationId);

    /** 读取明确指定且确实属于该 Information 的 Snapshot。 */
    @Select("""
            SELECT snapshot.id AS snapshot_id,
                   item.id AS information_id,
                   item.information_type,
                   snapshot.title,
                   snapshot.content,
                   snapshot.standardized_payload
            FROM information_item item
            JOIN information_snapshot snapshot
              ON snapshot.information_id = item.id
            WHERE item.id = #{informationId}
              AND snapshot.id = #{snapshotId}
            """)
    AnalysisSnapshotRow selectExplicit(
            @Param("informationId") long informationId,
            @Param("snapshotId") long snapshotId);
}
