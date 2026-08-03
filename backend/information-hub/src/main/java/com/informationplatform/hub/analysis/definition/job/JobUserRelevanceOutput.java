package com.informationplatform.hub.analysis.definition.job;

import java.math.BigDecimal;
import java.util.List;

public record JobUserRelevanceOutput(
        /** 输出契约版本，固定为 1。 */
        int schemaVersion,
        /** 职位与用户偏好的相关度，范围 0 至 100。 */
        int relevanceScore,
        /** 模型对分析结果的置信度，范围 0 至 1。 */
        BigDecimal confidence,
        /** 最长 1000 个 Unicode 字符的分析摘要。 */
        String summary,
        /** 支持相关性判断的积极信号，最多 20 项。 */
        List<String> positiveSignals,
        /** 降低相关性的消极信号，最多 20 项。 */
        List<String> negativeSignals,
        /** 需要用户进一步关注的信息，最多 20 项。 */
        List<String> attentionPoints,
        /** 与用户偏好匹配的条目，最多 20 项。 */
        List<String> matchedPreferences,
        /** 与用户偏好不匹配或未满足的条目，最多 20 项。 */
        List<String> unmatchedPreferences) {

    public JobUserRelevanceOutput {
        positiveSignals = List.copyOf(positiveSignals);
        negativeSignals = List.copyOf(negativeSignals);
        attentionPoints = List.copyOf(attentionPoints);
        matchedPreferences = List.copyOf(matchedPreferences);
        unmatchedPreferences = List.copyOf(unmatchedPreferences);
    }
}
