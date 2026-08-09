package com.informationplatform.hub.recommendation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.infrastructure.persistence.po.RecommendationRunPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 提供预计算 Recommendation Run 的基础持久化能力。 */
@Mapper
public interface RecommendationRunMapper extends BaseMapper<RecommendationRunPo> {

    /** 锁定同 Owner/Type 当前冲突的 Manual Run；Profile 行锁负责串行化空结果检查。 */
    @Select("""
            SELECT *
            FROM recommendation_run
            WHERE user_id = #{userId}
              AND information_type = #{informationType}
              AND trigger_type = 'MANUAL'
              AND status IN ('PENDING', 'RUNNING')
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            FOR UPDATE
            """)
    RecommendationRunPo selectActiveManualForUpdate(
            @Param("userId") long userId,
            @Param("informationType") String informationType);

    /** 按来源 Analysis Batch 唯一键读取既有 Auto Run。 */
    @Select("""
            SELECT *
            FROM recommendation_run
            WHERE source_analysis_batch_id = #{sourceAnalysisBatchId}
            """)
    RecommendationRunPo selectBySourceAnalysisBatchId(
            @Param("sourceAnalysisBatchId") long sourceAnalysisBatchId);

    /** 使用 SKIP LOCKED 并发安全领取一个最早 PENDING Run。 */
    @Select("""
            SELECT *
            FROM recommendation_run
            WHERE status = 'PENDING'
            ORDER BY created_at ASC, id ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """)
    RecommendationRunPo selectNextPendingForUpdate();

    /** Worker 完成、失败或恢复时锁定 Run。 */
    @Select("""
            SELECT *
            FROM recommendation_run
            WHERE id = #{runId}
            FOR UPDATE
            """)
    RecommendationRunPo selectByIdForUpdate(@Param("runId") long runId);

    /** 锁定超过恢复阈值的 RUNNING Run；确定性本地计算可安全回到 PENDING。 */
    @Select("""
            SELECT *
            FROM recommendation_run
            WHERE status = 'RUNNING'
              AND started_at < #{cutoff}
            ORDER BY started_at ASC, id ASC
            FOR UPDATE SKIP LOCKED
            """)
    List<RecommendationRunPo> selectStaleRunningForUpdate(
            @Param("cutoff") LocalDateTime cutoff);

    /** Owner + Information Type 查询详情；不存在与跨 Owner 统一返回空。 */
    @Select("""
            SELECT *
            FROM recommendation_run
            WHERE id = #{runId}
              AND user_id = #{userId}
              AND information_type = #{informationType}
            """)
    RecommendationRunPo selectOwnedByIdAndType(
            @Param("runId") long runId,
            @Param("userId") long userId,
            @Param("informationType") String informationType);

    /** 按 Owner + Information Type 返回最近 Run，不跨领域混合。 */
    @Select("""
            SELECT *
            FROM recommendation_run
            WHERE user_id = #{userId}
              AND information_type = #{informationType}
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<RecommendationRunPo> selectOwnedByType(
            @Param("userId") long userId,
            @Param("informationType") String informationType,
            @Param("limit") int limit);
}
