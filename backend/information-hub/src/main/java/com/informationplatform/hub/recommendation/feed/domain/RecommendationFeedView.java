package com.informationplatform.hub.recommendation.feed.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 当前 Owner 最新成功 Recommendation Run 的可见分页投影。 */
public record RecommendationFeedView(
        /** 当前 Feed 来源 Run；没有成功 Run 时为空。 */ Run run,
        /** 从 1 开始的页码。 */ int page,
        /** 单页数量，范围 1..100。 */ int pageSize,
        /** 当前 Interaction visibility 生效后的可见 Item 总数。 */ long total,
        /** 当前页可见 Recommendation Item。 */ List<Item> items) {

    /** Feed 暴露的成功 Run 身份与 Profile 新鲜度。 */
    public record Run(
            /** Recommendation Run 主键。 */ long id,
            /** Run 冻结的 Information Type。 */ String informationType,
            /** MANUAL 或 ANALYSIS_BATCH_COMPLETED。 */ String triggerType,
            /** Run 完成时间，按 UTC 保存。 */ LocalDateTime completedAt,
            /** 推荐算法稳定标识。 */ String algorithmKey,
            /** 推荐算法版本。 */ int algorithmVersion,
            /** 当前 Profile 是否已不同于 Run 冻结 Profile。 */ boolean profileChangedSinceRun) {}

    /** Feed 中一个预计算 Recommendation Item 与 current Interaction 投影。 */
    public record Item(
            /** Recommendation Item 主键，可用于 Interaction 归因。 */ long recommendationItemId,
            /** 被推荐 Information 主键。 */ long informationId,
            /** Run 计算时绑定的不可变 Snapshot 主键。 */ long snapshotId,
            /** Run 内从 1 开始的稳定排名。 */ int rank,
            /** 最终推荐分数，范围 0..100。 */ BigDecimal finalScore,
            /** 可解释评分分项。 */ ScoreBreakdown scoreBreakdown,
            /** 稳定推荐原因列表。 */ List<String> reasons,
            /** 当前通用 Feedback 状态。 */ String feedbackState,
            /** 当前 JOB disposition 状态。 */ String jobDisposition,
            /** 当前用户是否查看过该 Information。 */ boolean viewed,
            /** 当前职位展示字段。 */ Job job) {}

    /** Recommendation Item 的三个确定性评分分项。 */
    public record ScoreBreakdown(
            /** AI USER_RELEVANCE 分数。 */ BigDecimal aiRelevanceScore,
            /** 结构化 JOB Profile 匹配分数。 */ BigDecimal profileMatchScore,
            /** frozen window 内的新鲜度分数。 */ BigDecimal freshnessScore) {}

    /** 复用现有 Job Query DTO 语义的轻量职位展示字段。 */
    public record Job(
            /** 职位标题。 */ String title,
            /** 公司名称。 */ String companyName,
            /** 来源展示的薪资文本。 */ String salaryText,
            /** 来源展示的完整工作地点。 */ String locationName,
            /** 办公方式。 */ String remoteType,
            /** 来源职位详情页地址。 */ String sourceUrl) {}
}
