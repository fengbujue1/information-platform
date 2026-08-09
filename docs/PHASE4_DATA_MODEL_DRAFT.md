# Phase 4 Data Model

状态：Accepted

文件名保留 `_DRAFT` 仅用于兼容既有文档命名方式。

## 1. 新增核心表

```text
user_recommendation_profile
user_information_interaction
recommendation_run
recommendation_item
```

不新增：

```text
recommendation_schedule
recommendation_batch
recommendation_profile_version
```

## 2. 关系

```text
user_account
    │
    ├── ai_prompt_profile
    │       └── ai_prompt_version
    │
    ├── user_recommendation_profile
    │       └── binds ai_prompt_profile
    │
    ├── user_information_interaction
    │       └── information_item
    │
    └── recommendation_run
            ├── ai_prompt_version
            ├── source ai_analysis_batch (optional)
            └── recommendation_item
                    ├── information_item
                    ├── information_snapshot
                    └── information_analysis
```

## 3. UserRecommendationProfile

```text
id
userId
analysisPromptProfileId
windowDays
topN
targetRoles
preferredSkills
preferredCities
preferredRemoteTypes
salaryMinMonthlyYuan
excludedKeywords
contentHash
createdAt
updatedAt
```

一个 user 最多一个 current Profile。

Run 创建时冻结：

```text
profileContentHash
profileSnapshotJson
```

## 4. UserInformationInteraction

职责：

> 保存用户对一个 Information 的当前浏览、推荐反馈和求职处理状态。

```text
id
userId
informationId

viewCount
lastViewedAt

feedbackState
feedbackUpdatedAt

jobDisposition
dispositionUpdatedAt

lastRecommendationItemId

createdAt
updatedAt
```

### feedbackState

```text
NONE
INTERESTED
NOT_INTERESTED
```

### jobDisposition

```text
NONE
CONTACTED
CONTACTED_NOT_SUITABLE
```

唯一：

```text
(userId, informationId)
```

Hard exclusion：

```text
feedbackState == NOT_INTERESTED
OR
jobDisposition == CONTACTED_NOT_SUITABLE
```

`CONTACTED` 不排除。

Interaction 绑定 `informationId`，因此职位更新 Snapshot 后状态仍保留。

Phase 4 V1 不保存完整行为 Event Log。

## 5. RecommendationRun

```text
id
userId
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

Trigger：

```text
ANALYSIS_BATCH_COMPLETED
MANUAL
```

Status：

```text
PENDING
RUNNING
COMPLETED
FAILED
NOOP
```

## 6. RecommendationItem

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

Item 是推荐历史事实。

Interaction 修改：

- 不更新 Item；
- 不删除 Item；
- Feed Query 可基于 Interaction 隐藏；
- 下一 Run Candidate 也基于 Interaction 排除。

## 7. Algorithm Identity

```text
algorithmKey = JOB_RECOMMENDATION
algorithmVersion = 1
```

Algorithm V1 冻结：

- candidate semantics；
- 70/20/10 weights；
- profile match；
- freshness；
- duplicate key；
- diversity；
- tie breaker；
- hard exclusion definitions。

重大不兼容变化升级 Algorithm Version。

## 8. Owner Boundary

Owner 来自 Session。

以下都必须 Owner 隔离：

- Profile；
- Interaction；
- Run；
- Item / Feed。

客户端 userId 不可信。

## 9. 历史语义

历史 Run 能回答：

- 使用哪个 Prompt Version；
- 当时 Profile；
- Algorithm Version；
- Snapshot；
- Analysis；
- score breakdown；
- reasons；
- rank。

当前 Interaction 可改变 Feed visibility，但不篡改历史 Run/Item。

## 10. BOSS 联系状态边界

Phase 4 不把 BOSS 聊天记录接入数据模型。

`CONTACTED` / `CONTACTED_NOT_SUITABLE` 是用户在 Information Platform 中手工维护的业务状态。

如果来源职位以后以新的 source item identity 重新发布，它会成为新的 Information，V1 不保证自动继承旧 Information 的 disposition。
