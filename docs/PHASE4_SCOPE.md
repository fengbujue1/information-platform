# Phase 4：Personalized Recommendation MVP

状态：Accepted

适用阶段：Phase 4

前置条件：Phase 3 已完成

## 1. 定位

Phase 3 已解决：

```text
User
→ Prompt Profile / Version
→ JOB_USER_RELEVANCE
→ Analysis
```

Phase 4 解决：

```text
Analysis
+
Recommendation Profile
+
Job Facts
+
User Interaction
        ↓
Recommendation Candidate
        ↓
Scoring
        ↓
Ranking / Dedup / Diversity
        ↓
Top N
        ↓
Recommendation Feed
```

Phase 3 负责“理解 Information 对用户的相关性”。

Phase 4 负责“利用已经存在的理解结果主动生成推荐”。

## 2. Phase 4 正式范围

### 2.1 Backend Operational Logging Baseline

Recommendation 开发前先建立统一业务流程日志：

- HTTP 生命周期；
- requestId；
- 登录 username MDC；
- validation / business rejection；
- Prompt；
- Collector / Archive；
- Single Analysis；
- Analysis Batch；
- Schedule；
- AI Provider start/end/duration；
- Recommendation Run；
- 异步 MDC；
- ERROR stack trace。

详细规范：`LOGGING_CONVENTIONS.md`。

### 2.2 Recommendation Profile

一个用户最多一个当前 Recommendation Profile。

V1：

```text
analysisPromptProfileId
windowDays
topN
targetRoles
preferredSkills
preferredCities
preferredRemoteTypes
salaryMinMonthlyYuan
excludedKeywords
```

Recommendation Profile 与 AI Prompt Profile 分离。

Recommendation Profile 显式绑定一个属于同一 Owner 的 AI Prompt Profile，用来确定 Recommendation 应消费哪一套 USER_RELEVANCE 语义。

Profile 修改：

- 只保存；
- 不自动 AI Analysis；
- 不自动 Recommendation Refresh；
- 在下一次 Auto / Manual Refresh 生效。

每次 Recommendation Run 冻结完整 Profile Snapshot 与 contentHash。

### 2.3 Candidate Resolver

V1 只做：

```text
informationType = JOB
```

Candidate 必须：

- 当前 Information / Job 仍可用；
- 使用当前 Snapshot；
- 位于 Profile window；
- 存在同一用户、同一 Prompt Version、同一当前 Snapshot 的成功 `JOB_USER_RELEVANCE` Analysis；
- 未命中 hard exclusion；
- 未命中 `excludedKeywords`。

Hard exclusion：

```text
feedbackState = NOT_INTERESTED
OR
jobDisposition = CONTACTED_NOT_SUITABLE
```

### 2.4 Recommendation Scoring V1

```text
finalScore =
    aiRelevanceScore × 70%
  + profileMatchScore × 20%
  + freshnessScore × 10%
```

其中：

- AI relevance 读取 Phase 3 `JOB_USER_RELEVANCE.relevanceScore`；
- Profile Match 使用结构化规则；
- Freshness 使用 Information 首次进入平台时间；
- 未配置的 Profile 维度不作为负分；
- `excludedKeywords` 是 hard exclusion。

V1 不调用新的 LLM。

### 2.5 Ranking / Deduplication / Diversity / Top N

V1 必须实现：

- stable ranking；
- deterministic duplicate key；
- duplicate elimination；
- basic company/title diversity；
- Top N；
- score breakdown；
- recommendation reasons。

不使用 Embedding。

### 2.6 Recommendation Run

推荐不是 Feed GET 时实时计算。

流程：

```text
Trigger
→ recommendation_run(PENDING)
→ Worker
→ RUNNING
→ Candidate / Score / Rank
→ recommendation_item
→ COMPLETED
```

Feed 只读取最近一次成功 COMPLETED Run。

新 Run：

- PENDING/RUNNING 时旧 Feed 可继续读取；
- FAILED 时旧 Feed 继续读取；
- COMPLETED 后 Feed 切换。

### 2.7 Recommendation Refresh Trigger

只允许两个业务入口。

#### A. Analysis Batch 完成自动刷新

Phase 3 的：

```text
MANUAL
SCHEDULED
```

Analysis Batch 在：

```text
COMPLETED
PARTIAL_FAILED
```

时，如果：

- Recommendation Profile 存在；
- Owner 相同；
- Batch Prompt Profile 与 Recommendation Profile 绑定 Prompt Profile 相同；

则创建 Auto Recommendation Run。

`FAILED` / `NOOP` 不触发。

同一个 `sourceAnalysisBatchId` 最多产生一个 Auto Run。

#### B. 用户手动刷新

用户显式点击“刷新推荐”。

Manual Refresh：

- 使用当前 Recommendation Profile；
- 使用绑定 Prompt Profile 当前 Active Version；
- 只使用数据库已有成功 Analysis；
- 不先跑 Analysis Batch；
- 不调用 AI Provider。

### 2.8 Interaction / Feedback / Job Disposition

V1 保存两个互相独立的维度。

#### Feedback

```text
NONE
INTERESTED
NOT_INTERESTED
```

#### Job Disposition

```text
NONE
CONTACTED
CONTACTED_NOT_SUITABLE
```

语义：

`CONTACTED`：

> 用户已经通过 BOSS 原始链接联系过该岗位，但仍可能继续沟通。

它不自动排除推荐。

`CONTACTED_NOT_SUITABLE`：

> 用户联系/沟通后确认该岗位不合适。

它是 hard exclusion。

行为：

```text
NOT_INTERESTED
CONTACTED_NOT_SUITABLE
        ↓
当前 Feed 查询立即隐藏
        +
后续 Candidate Resolver 永久排除
```

“永久”是指在用户不主动修改状态前，跨 Recommendation Run、跨 Snapshot 持续生效。

用户可以恢复为：

```text
feedbackState = NONE
jobDisposition = NONE
```

以取消排除。

Phase 4 不自动读取 BOSS 聊天记录。状态由用户在 Information Platform 手工标记。

### 2.9 Recommendation Feed Web

Phase 4 Web 必须支持：

- Recommendation Profile；
- Feed；
- final score；
- score breakdown；
- reasons；
- Manual Refresh；
- Run 状态；
- Interested；
- Not Interested；
- 已联系；
- 已联系且不合适；
- 已查看；
- Profile stale 提示。

Hard exclusion 操作后当前卡片应立即从 Feed 消失，不要求先重新生成 Recommendation Run。

`CONTACTED` 应保留卡片并展示状态。

本阶段只要求功能完整，不做全站 UI/UX 重构。

## 3. 明确不做

- Recommendation LLM；
- RAG；
- Embedding；
- Vector DB；
- Elasticsearch；
- Hybrid Search；
- Rerank Model；
- Kafka / RabbitMQ / Redis；
- 微服务拆分；
- ML Learning-to-Rank；
- Collaborative Filtering；
- 自动从 Feedback 学习 Recommendation Profile；
- 自动读取 BOSS 聊天记录；
- BOSS Browser Extension；
- 独立 Recommendation Cron；
- Profile 修改自动 Refresh；
- Information 入库实时 Refresh；
- Feed GET 实时重算；
- Notification；
- 邮件/微信/短信；
- 多 Information Type Recommendation 实际实现；
- 全站 UI 改版；
- 公网生产部署。

## 4. Phase 4 验收链路

```text
Collector
→ Information / Snapshot
→ Phase 3 Analysis Batch
→ USER_RELEVANCE
→ Batch COMPLETED/PARTIAL_FAILED
→ Auto Recommendation Run
→ Candidate
→ Scoring
→ Ranking / Dedup / Diversity
→ Recommendation Item
→ Feed
→ Feedback / Job Disposition
```

以及：

```text
User
→ Profile
→ Manual Refresh
→ Run
→ Feed
```

以及：

```text
User opens BOSS link
→ manually marks CONTACTED
→ still visible

User confirms not suitable
→ marks CONTACTED_NOT_SUITABLE
→ current Feed immediately hides
→ future Runs exclude
```

## 5. 完成标准

- TASK-033 ～ TASK-044 完成；
- Phase 4 Flyway / Mapper / Integration Tests 通过；
- Owner 隔离通过；
- Auto Trigger 幂等；
- Manual Refresh 不产生 Provider 调用；
- FAILED/NOOP Batch 不刷新；
- PARTIAL_FAILED 可使用成功 Analysis 推荐；
- FAILED Recommendation Run 不覆盖旧 Feed；
- Profile 修改不自动 Refresh；
- `NOT_INTERESTED` 当前 Feed 立即隐藏且未来排除；
- `CONTACTED_NOT_SUITABLE` 当前 Feed 立即隐藏且未来排除；
- `CONTACTED` 不自动排除；
- 状态恢复为 NONE 后 hard exclusion 取消；
- score breakdown / reasons 可解释；
- 后端关键失败路径有日志；
- Fake Provider E2E 覆盖 Analysis Batch → Recommendation；
- Phase 4 文档收尾完成。
