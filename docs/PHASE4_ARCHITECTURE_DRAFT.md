# Phase 4 Architecture — Personalized Recommendation MVP

状态：Accepted

文件名保留 `_DRAFT` 仅为延续项目既有命名习惯，不表示仍待讨论。

## 1. 总体结构

```text
                         Phase 3
┌────────────────────────────────────────────────────┐
│ AI Prompt Profile / Version                        │
│        ↓                                           │
│ Analysis Batch                                     │
│        ↓                                           │
│ JOB_USER_RELEVANCE                                 │
│        ↓                                           │
│ information_analysis                               │
└──────────────────────┬─────────────────────────────┘
                       │
                       │ terminal event
                       ▼
                         Phase 4
┌────────────────────────────────────────────────────┐
│ recommendation                                     │
│                                                    │
│ RecommendationProfileCore                          │
│      └── JobRecommendationProfile                  │
│              ├── CandidateResolver ───────┐        │
│              └── Scoring / Ranking ───────┼→ Run   │
│                                           │  Items │
│ UserInformationInteractionCore ───────────┘        │
│      └── UserJobDisposition                        │
│                                                    │
│ RecommendationWorker → FeedQuery                   │
└────────────────────────────────────────────────────┘
                       │
                       ▼
                      MySQL
```

继续采用模块化单体，不拆微服务。

## 2. Recommendation 与 Analysis 边界

Recommendation 可以读取：

- identity；
- information；
- job；
- analysis。

Recommendation 不允许：

- 修改 `information_analysis`；
- 直接调用 AI Provider；
- 重写 Prompt Assembly；
- 修改来源 Information / Snapshot。

Phase 3 产出理解结果，Phase 4 消费。

Recommendation 内部采用 ADR-018：

```text
Generic Recommendation Core
        ↓
Information-Type-specific Profile / Interaction / Candidate / Algorithm
```

Phase 4 当前只实现 JOB，不实现其它领域。

## 3. Precomputed Recommendation

Feed 请求：

```text
GET Feed
→ latest COMPLETED Run
→ persisted Items
→ current Interaction projection/filter
→ return
```

不做：

```text
GET Feed
→ database-wide candidate scan
→ score
→ rank
→ return
```

## 4. Recommendation Profile Core / Domain Extension / AI Prompt Profile

```text
User
├── AI Prompt Profile
│   └── USER_RELEVANCE semantics
│
└── Recommendation Profile Core (per informationType)
    ├── binds analysisPromptProfileId
    ├── generic windowDays / topN / contentHash
    └── Job Recommendation Profile Extension
            └── roles / skills / cities / remote / salary / exclusions
```

Core 按 `(userId, informationType)` 唯一并绑定 Prompt Profile，避免不同领域或 Prompt 用途的 Analysis 被混算。JOB Extension 与 Core 组成完整 Profile；Run hash/snapshot 必须覆盖两者。

## 5. Prompt Version

Auto Run：

```text
RecommendationRun.promptVersionId
=
sourceAnalysisBatch.promptVersionId
```

Manual Run：

```text
Recommendation Profile
→ bound Prompt Profile
→ current Active Prompt Version
→ freeze into Run
```

Manual Refresh 不回退旧 Version，也不自动补跑 AI。

## 6. Candidate Resolver

输入：

```text
userId
informationType
promptVersionId
profileSnapshot
windowStart
windowEnd
```

核心条件：

```text
JOB
AND current usable information
AND current Snapshot
AND in window
AND successful JOB_USER_RELEVANCE
    same user
    same promptVersionId
    same current snapshot
AND generic feedbackState != NOT_INTERESTED
AND JOB disposition != CONTACTED_NOT_SUITABLE
AND not excluded by excludedKeywords
```

Hard exclusion 基于：

```text
(userId, informationId)
```

因此不因 Snapshot 更新而丢失。

## 7. Interaction Core 与 JOB Disposition Extension

`user_information_interaction` 是用户 × Information 的通用 current aggregate state，只保存 view、feedback 和最近归因。

```text
UserInformationInteractionCore.feedbackState:
  NONE / INTERESTED / NOT_INTERESTED

UserJobDisposition.jobDisposition:
  NONE / CONTACTED / CONTACTED_NOT_SUITABLE
```

两者独立。

示例：

```text
INTERESTED + CONTACTED
```

表示：

> 用户感兴趣，并且已经联系，仍在沟通。

也允许：

```text
INTERESTED + CONTACTED_NOT_SUITABLE
```

Job disposition 通过 `interactionId` 1:1 扩展，Service 校验对应 Information 为 JOB。Recommendation hard exclusion 仍以 `CONTACTED_NOT_SUITABLE` 优先。

前端在标记“不合适”时可选择同时把 feedbackState 改为 NOT_INTERESTED，但 Backend Contract 不强制耦合两个字段。

## 8. 当前 Feed 即时隐藏

Recommendation Item 历史不删除。

Feed Query 在读取 Item 后应用当前 Interaction hard exclusion：

```text
NOT_INTERESTED
OR
CONTACTED_NOT_SUITABLE
```

因此：

```text
Run #100 已经 COMPLETED
→ Item B 已存在

用户将 B 标记 CONTACTED_NOT_SUITABLE
→ recommendation_item 不删除
→ Feed 不再返回 B
```

这样：

- 历史 Run 可审计；
- 当前产品体验立即生效；
- 下一轮 Candidate 也不会再次进入。

恢复状态为 NONE 后，如果 Item 仍属于当前成功 Run，可以再次出现在 Feed。

## 9. JOB Algorithm V1

```text
AI relevance       70%
Profile match      20%
Freshness          10%
```

### AI relevance

读取成功 USER_RELEVANCE output：

```text
relevanceScore 0..100
```

### Profile Match

确定性规则：

- target role；
- preferred skill；
- city；
- remote type；
- salary minimum。

未配置的维度不参与分母。

### Freshness

基于 `information_item.first_seen_time` 在 frozen window 中归一化。

### Ordering

```text
finalScore DESC
freshnessScore DESC
informationId DESC
```

该算法只属于 `JOB_RECOMMENDATION / V1`；未来 Information Type 使用独立 Algorithm Key/Version。

## 10. Deduplication

不使用 Embedding。

V1 duplicate key：

```text
normalizedCompanyName
+
normalizedTitle
+
normalizedCityName
```

同组保留最高分。

用户 hard exclusion 绑定 Information，而不是 duplicate group。

因此 BOSS 以全新 source ID 重新发布的职位可能被视为新的 Information；Phase 4 V1 不做“跨新发布记录永久语义封禁”。

## 11. Diversity

两阶段：

第一阶段：

- 同 company 前 20 最多 3；
- 同 normalized title group 前 20 最多 5。

第二阶段：

- 如不足 topN，按原始排序补充；
- 仍遵守 duplicate 去重。

## 12. Run Lifecycle

Run 显式冻结 `informationType`，所有 Owner Run/Feed 查询同时按 Information Type 隔离。

```text
PENDING
RUNNING
COMPLETED
FAILED
NOOP
```

```text
Trigger
→ create PENDING
→ Worker claim
→ RUNNING
→ read/calculate outside long tx
→ short final tx:
   insert Items
   complete Run
```

## 13. Worker

可用 Spring Scheduled Polling 检查 PENDING Run。

它只负责执行已创建 Run：

```text
PENDING
→ claim
```

不得自行创建“今天应该给哪个用户刷新”的 Run。

因此 Worker Polling 不是独立 Recommendation Cron。

## 14. Auto Trigger

```text
Analysis Batch
→ terminal
→ application event
→ AFTER_COMMIT listener
→ eligibility
→ create Run
```

Eligible terminal status：

```text
COMPLETED
PARTIAL_FAILED
```

Eligibility：

```text
Profile exists
Owner matches
Batch Prompt Profile == Profile.analysisPromptProfileId
```

`FAILED` / `NOOP` skip。

DB UNIQUE：

```text
source_analysis_batch_id
```

保证同一 Batch Auto Trigger 幂等。

## 15. Manual Refresh

```text
POST refresh
→ current Owner
→ informationType = JOB
→ current JOB Profile Core + Extension
→ bound Prompt Profile Active Version
→ create PENDING Run
→ 202
```

不调用：

```text
Analysis Preview
Analysis Batch
AiProviderClient
```

## 16. Feed Visibility

基础 Feed：

```text
latest COMPLETED Run
same owner + informationType
```

然后应用 current Interaction visibility：

```text
generic NOT_INTERESTED               -> hidden
JOB CONTACTED_NOT_SUITABLE           -> hidden
JOB CONTACTED                        -> visible + badge
```

因此 current feed total 是当前可见 Item 数，不必等下一 Recommendation Run。

## 17. Profile Stale

Run：

```text
profileContentHash
profileSnapshotJson
```

Feed 比较 current Profile hash。

不同：

```text
profileChangedSinceRun = true
```

只提示，不自动刷新。

## 18. Failure

- Recommendation Run FAILED 不删除旧 Feed；
- PENDING 可在进程重启后继续处理；
- stale RUNNING 需要 recovery；
- 本地确定性计算允许 retry；
- COMPLETED Run 不覆盖重算；
- Item 历史不因 Interaction 变化物理删除。

## 19. 日志

关键：

```text
Analysis batch completed
Recommendation trigger evaluated
Run created
Run started
Candidates resolved
Scoring completed
Run completed/failed
Feed requested
Interaction updated
Job disposition updated
```

统一遵守 `LOGGING_CONVENTIONS.md`。

## 20. 不引入

- Kafka；
- Redis；
- Elasticsearch；
- Vector DB；
- Spring Batch；
- Quartz；
- Recommendation Service；
- BOSS 聊天记录同步。
