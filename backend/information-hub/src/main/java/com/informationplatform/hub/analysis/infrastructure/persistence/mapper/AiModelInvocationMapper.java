package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.batch.infrastructure.persistence.BatchUsageAggregateRow;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiModelInvocationPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 提供模型调用及 Provider 实际用量记录的基础持久化能力。 */
@Mapper
public interface AiModelInvocationMapper extends BaseMapper<AiModelInvocationPo> {
    /** 返回已锁定 Analysis 下的最大 attempt；调用方持有 Analysis 行锁。 */
    @Select("""
            SELECT COALESCE(MAX(attempt_no), 0)
            FROM ai_model_invocation
            WHERE analysis_id = #{analysisId}
            """)
    int selectMaxAttemptNo(@Param("analysisId") long analysisId);

    /** 在完成短事务中锁定本次 Invocation，并同时验证 Analysis 与 Owner。 */
    @Select("""
            SELECT * FROM ai_model_invocation
            WHERE id = #{invocationId}
              AND analysis_id = #{analysisId}
              AND user_id = #{userId}
            FOR UPDATE
            """)
    AiModelInvocationPo selectForUpdate(
            @Param("invocationId") long invocationId,
            @Param("analysisId") long analysisId,
            @Param("userId") long userId);

    /** 锁定某 Batch Item 遗留的 RUNNING Invocation，供重启恢复标记 UNKNOWN。 */
    @Select("""
            SELECT * FROM ai_model_invocation
            WHERE batch_item_id = #{batchItemId}
              AND status = 'RUNNING'
            ORDER BY attempt_no DESC
            LIMIT 1
            FOR UPDATE
            """)
    AiModelInvocationPo selectRunningByBatchItemForUpdate(
            @Param("batchItemId") long batchItemId);

    /** 只从绑定本 Batch Item 的 Provider Invocation 聚合 Actual Usage。 */
    @Select("""
            SELECT
                SUM(CASE WHEN invocation.usage_status = 'REPORTED' THEN 1 ELSE 0 END)
                    AS reported_invocation_count,
                SUM(CASE WHEN invocation.usage_status <> 'REPORTED' THEN 1 ELSE 0 END)
                    AS unavailable_invocation_count,
                SUM(CASE WHEN invocation.usage_status = 'REPORTED'
                    THEN invocation.input_tokens ELSE NULL END) AS input_tokens,
                SUM(CASE WHEN invocation.usage_status = 'REPORTED'
                    THEN invocation.output_tokens ELSE NULL END) AS output_tokens,
                SUM(CASE WHEN invocation.usage_status = 'REPORTED'
                    THEN invocation.total_tokens ELSE NULL END) AS total_tokens
            FROM ai_model_invocation invocation
            JOIN ai_analysis_batch_item item
              ON item.id = invocation.batch_item_id
            JOIN ai_analysis_batch batch
              ON batch.id = item.batch_id
            WHERE batch.id = #{batchId}
              AND batch.user_id = #{userId}
            """)
    BatchUsageAggregateRow aggregateBatchUsage(
            @Param("batchId") long batchId,
            @Param("userId") long userId);
}
