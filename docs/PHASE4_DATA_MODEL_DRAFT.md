# Phase 4 Data Model

状态：Accepted

实施状态：TASK-034A 已通过 V4 完成通用化纠偏；TASK-038～039 已实现 JOB 确定性评分与 Ranking，尚未创建 Run / Item 业务链路

文件名保留 `_DRAFT` 仅用于兼容既有文档命名方式。

## 1. Generic Core + Domain Extension

```text
user_recommendation_profile       generic core
└── job_recommendation_profile    JOB extension

user_information_interaction      generic core
└── user_job_disposition          JOB extension

recommendation_run                generic core + informationType
└── recommendation_item           immutable result fact
```

不新增 Recommendation Schedule、Batch 或 Profile Version。Phase 4 当前只实际使用 `informationType = JOB`。

## 2. 关系

```text
user_account
    ├── ai_prompt_profile
    │       └── ai_prompt_version
    ├── user_recommendation_profile
    │       ├── binds ai_prompt_profile
    │       └── job_recommendation_profile (JOB only)
    ├── user_information_interaction
    │       ├── information_item
    │       └── user_job_disposition (JOB only)
    └── recommendation_run
            ├── informationType
            ├── ai_prompt_version
            ├── source ai_analysis_batch (optional)
            └── recommendation_item
                    ├── information_item
                    ├── information_snapshot
                    └── information_analysis
```

## 3. Recommendation Profile Core

```text
id
userId
informationType
analysisPromptProfileId
windowDays
topN
contentHash
createdAt
updatedAt
```

唯一：

```text
(userId, informationType)
```

`contentHash` 是 Core + 对应领域扩展的完整 canonical hash，不是仅 Core hash。

## 4. Job Recommendation Profile Extension

```text
profileId (PK + FK)
targetRoles
preferredSkills
preferredCities
preferredRemoteTypes
salaryMinMonthlyYuan
excludedKeywords
createdAt
updatedAt
```

Service 必须保证只关联 `informationType = JOB` 的 Core。Phase 4 不创建其它 Information Type 扩展。

## 5. User Information Interaction Core

```text
id
userId
informationId
viewCount
lastViewedAt
feedbackState
feedbackUpdatedAt
lastRecommendationItemId
createdAt
updatedAt
```

唯一 `(userId, informationId)`。通用 Feedback：

```text
NONE
INTERESTED
NOT_INTERESTED
```

通用 hard exclusion 只有 `NOT_INTERESTED`。Interaction 绑定 Information，不因 Snapshot 更新丢失。

## 6. User Job Disposition Extension

```text
interactionId (PK + FK)
jobDisposition
dispositionUpdatedAt
createdAt
updatedAt
```

JOB 状态：

```text
NONE
CONTACTED
CONTACTED_NOT_SUITABLE
```

`CONTACTED` 不排除；`CONTACTED_NOT_SUITABLE` 是 JOB hard exclusion。Service 必须校验 Interaction 对应 `information_item.information_type = JOB`。

## 7. Recommendation Run

```text
id
userId
informationType
triggerType
sourceAnalysisBatchId
profileId
profileContentHash
profileSnapshotJson
promptProfileId
promptVersionId
algorithmKey
algorithmVersion
windowStart
windowEnd
candidateCount
eligibleCount
resultCount
status
skipReason
failureCode
failureMessage
startedAt
completedAt
createdAt
updatedAt
```

Run 显式冻结 Information Type。`profileSnapshotJson` 保存完整组合 Profile，JOB 至少包含 Core 字段和全部 JOB 偏好。

Trigger：`ANALYSIS_BATCH_COMPLETED` / `MANUAL`。Status：`PENDING` / `RUNNING` / `COMPLETED` / `FAILED` / `NOOP`。

## 8. Recommendation Item

Item 结构保持：

```text
id
runId
informationId
snapshotId
analysisId
rankNo
finalScore
aiRelevanceScore
profileMatchScore
freshnessScore
scoreBreakdownJson
reasonsJson
duplicateGroupKey
createdAt
```

Item 是不可变历史事实；Interaction 或 Job disposition 变化不修改、删除 Item。

## 9. Algorithm Boundary

Phase 4 当前：

```text
informationType = JOB
algorithmKey = JOB_RECOMMENDATION
algorithmVersion = 1
70% AI relevance + 20% JOB profile match + 10% freshness
```

该规则不是平台所有 Information Type 的统一算法。未来领域使用独立 Algorithm Key/Version。

## 10. Owner 与历史语义

- Owner 只来自 Session；客户端 userId 不可信；
- Profile 以 Owner + Information Type 隔离；
- Interaction 以 Owner + Information 隔离；
- Run/Feed 同时按 Owner + Information Type 隔离；
- 历史 Run 必须能回答 Information Type、Prompt Version、完整 Profile snapshot、Algorithm、Snapshot、Analysis、score、reasons 和 rank。

## 11. BOSS 边界

Phase 4 不读取 BOSS 聊天记录。JOB disposition 由用户手工维护；新的 source item identity 是新的 Information，V1 不保证继承旧 disposition。
