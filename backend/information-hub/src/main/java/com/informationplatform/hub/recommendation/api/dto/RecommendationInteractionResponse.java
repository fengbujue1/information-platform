package com.informationplatform.hub.recommendation.api.dto;

import com.informationplatform.hub.recommendation.domain.UserInformationInteraction;
import java.time.LocalDateTime;

/** Interaction 写入后返回的 current aggregate，不暴露 Owner userId。 */
public record RecommendationInteractionResponse(
        /** 被交互 Information 主键。 */
        long informationId,
        /** 累计查看次数。 */
        int viewCount,
        /** 最近查看时间，UTC；从未查看时为空。 */
        LocalDateTime lastViewedAt,
        /** 通用 Feedback current state。 */
        String feedbackState,
        /** 最近 Feedback 修改时间，UTC；从未修改时为空。 */
        LocalDateTime feedbackUpdatedAt,
        /** JOB disposition current state；无扩展时为 NONE。 */
        String jobDisposition,
        /** 最近 JOB disposition 修改时间，UTC；从未修改时为空。 */
        LocalDateTime dispositionUpdatedAt,
        /** 最近一次明确提供的 Recommendation Item 归因主键。 */
        Long lastRecommendationItemId,
        /** Interaction Core 最近更新时间，UTC。 */
        LocalDateTime updatedAt) {

    /** 从领域 current aggregate 创建 API 投影。 */
    public static RecommendationInteractionResponse from(UserInformationInteraction interaction) {
        return new RecommendationInteractionResponse(
                interaction.informationId(),
                interaction.viewCount(),
                interaction.lastViewedAt(),
                interaction.feedbackState().name(),
                interaction.feedbackUpdatedAt(),
                interaction.jobDisposition().name(),
                interaction.dispositionUpdatedAt(),
                interaction.lastRecommendationItemId(),
                interaction.updatedAt());
    }
}
