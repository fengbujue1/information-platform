package com.informationplatform.hub.recommendation.job.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 映射通用 Interaction Core 的 JOB 求职状态 1:1 扩展。 */
@TableName("user_job_disposition")
public class UserJobDispositionPo {

    /** Interaction Core 主键，同时作为本扩展表的输入型主键。 */
    @TableId(type = IdType.INPUT)
    private Long interactionId;

    /** JOB 状态：NONE、CONTACTED 或 CONTACTED_NOT_SUITABLE。 */
    private String jobDisposition;

    /** 最近一次 JOB 状态修改时间，按 UTC 保存。 */
    private LocalDateTime dispositionUpdatedAt;

    /** JOB disposition 扩展创建时间，按 UTC 保存。 */
    private LocalDateTime createdAt;

    /** JOB disposition 扩展最近更新时间，按 UTC 保存。 */
    private LocalDateTime updatedAt;

    public Long getInteractionId() { return interactionId; }
    public void setInteractionId(Long interactionId) { this.interactionId = interactionId; }
    public String getJobDisposition() { return jobDisposition; }
    public void setJobDisposition(String jobDisposition) { this.jobDisposition = jobDisposition; }
    public LocalDateTime getDispositionUpdatedAt() { return dispositionUpdatedAt; }
    public void setDispositionUpdatedAt(LocalDateTime dispositionUpdatedAt) {
        this.dispositionUpdatedAt = dispositionUpdatedAt;
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
