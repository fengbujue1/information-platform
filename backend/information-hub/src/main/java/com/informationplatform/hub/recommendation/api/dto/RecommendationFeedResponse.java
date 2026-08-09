package com.informationplatform.hub.recommendation.api.dto;

import com.informationplatform.hub.recommendation.feed.domain.RecommendationFeedView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Recommendation Feed V1 的分页响应。 */
public record RecommendationFeedResponse(
        /** 当前 Feed 来源 Run；没有成功 Run 时为空。 */ Run run,
        /** 从 1 开始的页码。 */ int page,
        /** 单页数量。 */ int pageSize,
        /** 应用 current visibility 后的总数。 */ long total,
        /** 当前页可见 Item。 */ List<Item> items) {

    public static RecommendationFeedResponse from(RecommendationFeedView view) {
        return new RecommendationFeedResponse(
                view.run() == null ? null : Run.from(view.run()),
                view.page(),
                view.pageSize(),
                view.total(),
                view.items().stream().map(Item::from).toList());
    }

    /** Feed 来源 Run 摘要。 */
    public record Run(
            /** Run 主键。 */ long id,
            /** Information Type。 */ String informationType,
            /** Run 触发类型。 */ String triggerType,
            /** UTC 完成时间。 */ Instant completedAt,
            /** 算法标识。 */ String algorithmKey,
            /** 算法版本。 */ int algorithmVersion,
            /** 当前 Profile 是否已变化。 */ boolean profileChangedSinceRun) {

        private static Run from(RecommendationFeedView.Run run) {
            return new Run(
                    run.id(),
                    run.informationType(),
                    run.triggerType(),
                    toInstant(run.completedAt()),
                    run.algorithmKey(),
                    run.algorithmVersion(),
                    run.profileChangedSinceRun());
        }
    }

    /** Feed Item、current Interaction 与 JOB 展示信息。 */
    public record Item(
            /** Recommendation Item 主键。 */ long recommendationItemId,
            /** Information 主键。 */ long informationId,
            /** 冻结 Snapshot 主键。 */ long snapshotId,
            /** Run 内排名。 */ int rank,
            /** 最终分数。 */ BigDecimal finalScore,
            /** 评分分项。 */ ScoreBreakdown scoreBreakdown,
            /** 推荐原因。 */ List<String> reasons,
            /** 当前 Feedback。 */ String feedbackState,
            /** 当前 JOB disposition。 */ String jobDisposition,
            /** 是否查看过。 */ boolean viewed,
            /** 当前 JOB 展示字段。 */ Job job) {

        private static Item from(RecommendationFeedView.Item item) {
            return new Item(
                    item.recommendationItemId(),
                    item.informationId(),
                    item.snapshotId(),
                    item.rank(),
                    item.finalScore(),
                    new ScoreBreakdown(
                            item.scoreBreakdown().aiRelevanceScore(),
                            item.scoreBreakdown().profileMatchScore(),
                            item.scoreBreakdown().freshnessScore()),
                    item.reasons(),
                    item.feedbackState(),
                    item.jobDisposition(),
                    item.viewed(),
                    new Job(
                            item.job().title(),
                            item.job().companyName(),
                            item.job().salaryText(),
                            item.job().locationName(),
                            item.job().remoteType(),
                            item.job().sourceUrl()));
        }
    }

    /** Feed V1 评分分项。 */
    public record ScoreBreakdown(
            /** AI 相关度。 */ BigDecimal aiRelevanceScore,
            /** Profile 匹配度。 */ BigDecimal profileMatchScore,
            /** 新鲜度。 */ BigDecimal freshnessScore) {}

    /** Feed V1 JOB 展示字段。 */
    public record Job(
            /** 职位标题。 */ String title,
            /** 公司名称。 */ String companyName,
            /** 薪资文本。 */ String salaryText,
            /** 地点文本。 */ String locationName,
            /** 办公方式。 */ String remoteType,
            /** 来源详情页。 */ String sourceUrl) {}

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
