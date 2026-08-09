package com.informationplatform.hub.recommendation.domain;

import com.informationplatform.hub.recommendation.job.domain.JobDisposition;
import java.time.LocalDateTime;

/** 当前用户与 Information 的通用交互状态及可选 JOB 扩展投影。 */
public record UserInformationInteraction(
        /** 被交互的 Information 主键。 */
        long informationId,
        /** 累计查看次数，最小值为 0。 */
        int viewCount,
        /** 最近一次查看时间，按 UTC 表达；从未查看时为空。 */
        LocalDateTime lastViewedAt,
        /** 通用推荐反馈 current state。 */
        FeedbackState feedbackState,
        /** 最近一次反馈替换时间，按 UTC 表达；从未修改时为空。 */
        LocalDateTime feedbackUpdatedAt,
        /** JOB 求职状态；非 JOB 或尚未创建扩展时为 NONE。 */
        JobDisposition jobDisposition,
        /** 最近一次 JOB disposition 替换时间，按 UTC 表达；从未修改时为空。 */
        LocalDateTime dispositionUpdatedAt,
        /** 最近一次明确提供的 Recommendation Item 归因主键，可为空。 */
        Long lastRecommendationItemId,
        /** Interaction Core 最近更新时间，按 UTC 表达。 */
        LocalDateTime updatedAt) {
}
