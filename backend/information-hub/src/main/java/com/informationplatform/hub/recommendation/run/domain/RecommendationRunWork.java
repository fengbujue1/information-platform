package com.informationplatform.hub.recommendation.run.domain;

import java.time.LocalDateTime;

/** Worker 已领取并冻结为 RUNNING 的 Recommendation Run 执行输入。 */
public record RecommendationRunWork(
        /** Recommendation Run 主键。 */ long runId,
        /** Run Owner 用户主键。 */ long userId,
        /** 冻结 Information Type。 */ String informationType,
        /** 冻结 Profile 主键。 */ long profileId,
        /** 冻结完整 Profile snapshot JSON。 */ String profileSnapshotJson,
        /** 冻结 Prompt Profile 主键。 */ long promptProfileId,
        /** 冻结 Prompt Version 主键。 */ long promptVersionId,
        /** 冻结算法 Key。 */ String algorithmKey,
        /** 冻结算法版本。 */ int algorithmVersion,
        /** UTC 候选窗口左闭起点。 */ LocalDateTime windowStart,
        /** UTC 候选窗口右开终点。 */ LocalDateTime windowEnd) {

    public RecommendationRunWork {
        if (runId <= 0
                || userId <= 0
                || !"JOB".equals(informationType)
                || profileId <= 0
                || profileSnapshotJson == null
                || profileSnapshotJson.isBlank()
                || promptProfileId <= 0
                || promptVersionId <= 0
                || !"JOB_RECOMMENDATION".equals(algorithmKey)
                || algorithmVersion != 1
                || windowStart == null
                || windowEnd == null
                || !windowStart.isBefore(windowEnd)) {
            throw new IllegalArgumentException("Recommendation Run work is invalid");
        }
    }
}
