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
