package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 提供手动与定时共用分析批次的基础持久化能力。 */
@Mapper
public interface AiAnalysisBatchMapper extends BaseMapper<AiAnalysisBatchPo> {

    /** 按 Owner 和 Manual Request 幂等键读取既有 Batch。 */
    @Select("""
            SELECT * FROM ai_analysis_batch
            WHERE user_id = #{userId}
              AND manual_request_id = #{manualRequestId}
            """)
    AiAnalysisBatchPo selectManual(
            @Param("userId") long userId,
            @Param("manualRequestId") String manualRequestId);

    /** 按 Schedule 计划点读取幂等 Batch。 */
    @Select("""
            SELECT * FROM ai_analysis_batch
            WHERE schedule_id = #{scheduleId}
              AND scheduled_for = #{scheduledFor}
            """)
    AiAnalysisBatchPo selectScheduled(
            @Param("scheduleId") long scheduleId,
            @Param("scheduledFor") java.time.LocalDateTime scheduledFor);

    /** 判断同一 Schedule 是否已有仍在执行的 Batch。 */
    @Select("""
            SELECT * FROM ai_analysis_batch
            WHERE schedule_id = #{scheduleId}
              AND status IN ('PENDING', 'RUNNING')
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    AiAnalysisBatchPo selectActiveScheduled(@Param("scheduleId") long scheduleId);

    /** 派生 Schedule 最近一次运行信息，不新增冗余 lastTriggeredAt。 */
    @Select("""
            SELECT * FROM ai_analysis_batch
            WHERE schedule_id = #{scheduleId}
            ORDER BY scheduled_for DESC, id DESC
            LIMIT 1
            """)
    AiAnalysisBatchPo selectLatestScheduled(@Param("scheduleId") long scheduleId);

    /** Worker 完成事务中锁定 Batch。 */
    @Select("""
            SELECT * FROM ai_analysis_batch
            WHERE id = #{batchId}
            FOR UPDATE
            """)
    AiAnalysisBatchPo selectByIdForUpdate(@Param("batchId") long batchId);

    /** Owner 查询 Batch；跨账号与不存在统一返回空。 */
    @Select("""
            SELECT * FROM ai_analysis_batch
            WHERE id = #{batchId}
              AND user_id = #{userId}
            """)
    AiAnalysisBatchPo selectOwnedById(
            @Param("batchId") long batchId,
            @Param("userId") long userId);
}
