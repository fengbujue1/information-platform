package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.AiAnalysisBatchItemPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 提供冻结批次候选明细的基础持久化能力。 */
@Mapper
public interface AiAnalysisBatchItemMapper extends BaseMapper<AiAnalysisBatchItemPo> {

    /**
     * 按 Batch 创建时间和候选顺序领取一个待执行 Item。
     *
     * <p>第一版 Worker 单并发；SKIP LOCKED 保留安全行锁语义。
     */
    @Select("""
            SELECT item.*
            FROM ai_analysis_batch_item item
            JOIN ai_analysis_batch batch ON batch.id = item.batch_id
            WHERE item.status = 'SELECTED'
              AND batch.status IN ('PENDING', 'RUNNING')
            ORDER BY batch.created_at, batch.id, item.selection_order
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """)
    AiAnalysisBatchItemPo selectNextForUpdate();

    /** 在完成事务内锁定指定 Item。 */
    @Select("""
            SELECT * FROM ai_analysis_batch_item
            WHERE id = #{itemId}
            FOR UPDATE
            """)
    AiAnalysisBatchItemPo selectByIdForUpdate(@Param("itemId") long itemId);

    /** 应用启动后的首次 Worker 轮询锁定全部遗留 RUNNING Item。 */
    @Select("""
            SELECT * FROM ai_analysis_batch_item
            WHERE status = 'RUNNING'
            ORDER BY batch_id, selection_order
            FOR UPDATE
            """)
    List<AiAnalysisBatchItemPo> selectRunningForUpdate();
}
