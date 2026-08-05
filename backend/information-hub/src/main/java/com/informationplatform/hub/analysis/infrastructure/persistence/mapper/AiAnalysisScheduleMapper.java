package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisSchedulePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 提供每日分析计划的基础持久化能力。 */
@Mapper
public interface AiAnalysisScheduleMapper extends BaseMapper<AiAnalysisSchedulePo> {

    /** Owner 查询 Schedule；不存在与跨账号统一返回空。 */
    @Select("""
            SELECT * FROM ai_analysis_schedule
            WHERE id = #{scheduleId}
              AND user_id = #{userId}
            """)
    AiAnalysisSchedulePo selectOwnedById(
            @Param("scheduleId") long scheduleId,
            @Param("userId") long userId);

    /** Owner 更新前锁定 Schedule，避免并发配置覆盖。 */
    @Select("""
            SELECT * FROM ai_analysis_schedule
            WHERE id = #{scheduleId}
              AND user_id = #{userId}
            FOR UPDATE
            """)
    AiAnalysisSchedulePo selectOwnedByIdForUpdate(
            @Param("scheduleId") long scheduleId,
            @Param("userId") long userId);

    /** Dispatcher 依索引顺序锁定一个到期计划，多实例竞争时跳过已锁行。 */
    @Select("""
            SELECT * FROM ai_analysis_schedule
            WHERE enabled = 1
              AND next_run_at <= #{now}
            ORDER BY next_run_at, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """)
    AiAnalysisSchedulePo selectNextDueForUpdate(
            @Param("now") java.time.LocalDateTime now);

    /** 显式更新启停状态和 nextRunAt，允许停用时把计划点写为 NULL。 */
    @Update("""
            UPDATE ai_analysis_schedule
            SET enabled = #{enabled},
                next_run_at = #{nextRunAt}
            WHERE id = #{scheduleId}
              AND user_id = #{userId}
            """)
    int updateStatusAndNextRun(
            @Param("scheduleId") long scheduleId,
            @Param("userId") long userId,
            @Param("enabled") boolean enabled,
            @Param("nextRunAt") java.time.LocalDateTime nextRunAt);

    /** Dispatcher 在持有 Schedule 行锁后推进下一 UTC 计划点。 */
    @Update("""
            UPDATE ai_analysis_schedule
            SET next_run_at = #{nextRunAt}
            WHERE id = #{scheduleId}
              AND enabled = 1
            """)
    int updateNextRunAt(
            @Param("scheduleId") long scheduleId,
            @Param("nextRunAt") java.time.LocalDateTime nextRunAt);
}
