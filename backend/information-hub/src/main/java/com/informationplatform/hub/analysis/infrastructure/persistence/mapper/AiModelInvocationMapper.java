package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
