package com.informationplatform.hub.analysis.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.analysis.infrastructure.persistence.po.InformationAnalysisPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 提供快照级逻辑分析结果的基础持久化能力。 */
@Mapper
public interface InformationAnalysisMapper extends BaseMapper<InformationAnalysisPo> {
    /** 按冻结五元身份锁定 Analysis，串行化状态和 Invocation attempt 分配。 */
    @Select("""
            SELECT * FROM information_analysis
            WHERE user_id = #{userId}
              AND snapshot_id = #{snapshotId}
              AND prompt_version_id = #{promptVersionId}
              AND analysis_definition_key = #{definitionKey}
              AND analysis_definition_version = #{definitionVersion}
            FOR UPDATE
            """)
    InformationAnalysisPo selectIdentityForUpdate(
            @Param("userId") long userId,
            @Param("snapshotId") long snapshotId,
            @Param("promptVersionId") long promptVersionId,
            @Param("definitionKey") String definitionKey,
            @Param("definitionVersion") int definitionVersion);

    /** 按 Owner 读取 Analysis；不存在和跨账号统一返回空。 */
    @Select("""
            SELECT * FROM information_analysis
            WHERE id = #{analysisId} AND user_id = #{userId}
            """)
    InformationAnalysisPo selectOwnedById(
            @Param("analysisId") long analysisId,
            @Param("userId") long userId);

    /** 在完成短事务内按 Owner 锁定 Analysis。 */
    @Select("""
            SELECT * FROM information_analysis
            WHERE id = #{analysisId} AND user_id = #{userId}
            FOR UPDATE
            """)
    InformationAnalysisPo selectOwnedByIdForUpdate(
            @Param("analysisId") long analysisId,
            @Param("userId") long userId);
}
