package com.informationplatform.hub.recommendation.job.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.informationplatform.hub.recommendation.job.infrastructure.persistence.po.UserJobDispositionPo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 提供 JOB disposition 扩展的基础持久化能力。 */
@Mapper
public interface UserJobDispositionMapper extends BaseMapper<UserJobDispositionPo> {

    /** 原子创建或覆盖 JOB disposition 扩展，不修改通用 Feedback。 */
    @Insert("""
            INSERT INTO user_job_disposition (
                interaction_id, job_disposition, disposition_updated_at
            ) VALUES (
                #{interactionId}, #{jobDisposition}, #{occurredAt}
            )
            ON DUPLICATE KEY UPDATE
                job_disposition = #{jobDisposition},
                disposition_updated_at = #{occurredAt}
            """)
    int replaceDisposition(
            @Param("interactionId") long interactionId,
            @Param("jobDisposition") String jobDisposition,
            @Param("occurredAt") LocalDateTime occurredAt);
}
