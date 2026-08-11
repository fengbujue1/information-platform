# Phase 4 Official Design Summary

状态：Accepted / Implementation Completed

实施状态：TASK-033～TASK-044（含 TASK-034A）已完成并由 TASK-044 验收

## 一句话目标

基于用户长期 Recommendation Profile、Phase 3 AI USER_RELEVANCE 结果、职位事实和用户反馈，生成可解释、可追踪、可去重并具有基础多样性的 Top N 职位推荐。

## 推荐不是实时计算

```text
Trigger
→ RecommendationRun
→ Candidate
→ Score
→ Rank
→ Persist RecommendationItem
→ Feed Query
```

Feed GET 只读成功的预计算结果。

## 自动刷新

```text
MANUAL / SCHEDULED Analysis Batch
→ COMPLETED / PARTIAL_FAILED
→ Auto RecommendationRun
```

要求来源 Batch 的 Prompt Profile 与 Recommendation Profile 绑定的 Prompt Profile 一致。

## 手动刷新

```text
User clicks Refresh
→ current Profile
→ current Active Prompt Version
→ existing successful Analysis
→ RecommendationRun
```

不调用 AI Provider。

## 用户已联系 BOSS 后的状态

Interaction Core 与 JOB Extension 共同表达两类状态：

```text
feedbackState
NONE / INTERESTED / NOT_INTERESTED
```

以及：

```text
jobDisposition
NONE / CONTACTED / CONTACTED_NOT_SUITABLE
```

行为：

```text
CONTACTED
→ 仍可推荐
→ Feed 显示“已联系”

CONTACTED_NOT_SUITABLE
→ 当前 Feed 立即隐藏
→ 后续 Candidate 永久排除

NOT_INTERESTED
→ 当前 Feed 立即隐藏
→ 后续 Candidate 永久排除
```

用户可以将状态恢复为 NONE，从而取消 hard exclusion。

## Algorithm V1

```text
70% AI relevance
20% structured profile match
10% freshness
```

然后执行：

```text
stable ranking
→ deterministic dedup
→ basic diversity
→ Top N
```

## Phase 4 数据表

当前 V4 共六张表：

```text
user_recommendation_profile
job_recommendation_profile
user_information_interaction
user_job_disposition
recommendation_run
recommendation_item
```

V3 首先创建四张表；TASK-034A 通过不可变的 V4 Migration 无损演进为 Generic Core + JOB Extension。
不增加 recommendation_schedule / recommendation_batch。

## 不做

- Recommendation LLM
- Embedding / Vector
- RAG
- ML Ranking
- Kafka / Redis
- 自动同步 BOSS 聊天记录
- Notification
- 全站 UI 改版
- 生产公网部署
