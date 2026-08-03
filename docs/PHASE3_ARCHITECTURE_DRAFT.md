# Phase 3 Architecture

状态：Accepted
接受日期：2026-08-03
说明：文件名保留 `_DRAFT` 以维持既有链接；本文内容已经 TASK-020 审查并冻结。

## 1. 总体结构

```text
Information Hub Web
    │ Same-origin Session + CSRF
    ▼
Information Hub modular monolith
    ├── identity
    ├── information
    ├── job
    └── analysis
        ├── definition
        ├── prompt
        ├── provider
        ├── candidate
        ├── batch
        ├── worker
        └── schedule
    │
    ├── MySQL
    └── OpenAI-compatible Provider（默认关闭）

BOSS Collector
    └── 独立 Bearer Token → ingestion
```

Phase 3 继续使用单个 Spring Boot 模块化单体和当前 MySQL，不引入消息队列、缓存、搜索引擎或独立 Worker 服务。

## 2. 通用核心与首个领域实现

通用核心使用 Information 语义：

- `AnalysisDefinition`；
- `InformationAnalysis`；
- `AiModelInvocation`；
- `AnalysisBatch`；
- `AnalysisSchedule`；
- `CandidateResolver`；
- `AiProviderClient`。

JOB 只提供：

- `JOB_USER_RELEVANCE_V1`；
- Snapshot Input Projection；
- JOB Candidate Resolver；
- Output Schema Validator。

未来新增信息类型时增加新的 Definition/Projection/Resolver，不改写通用 Batch、Provider 和 Usage 核心。

## 3. 模块依赖

```text
identity
   ↑ owner
prompt ─────────────┐
                    ▼
information ← analysis definition ← job implementation
                    │
                    ├── provider
                    ├── batch/worker
                    └── schedule
```

- `identity` 不依赖 analysis；
- `information` 不依赖具体 AI Provider；
- `job` 可以读取 information 标准化投影；
- `analysis` 不修改 information/job/snapshot 来源事实；
- Controller 只做协议适配，业务规则与事务在 Application Service；
- 特殊行锁查询放 Mapper 固定 SQL，不把 SQL 或 Mapper 调用放进 Controller。

## 4. Snapshot 读取路径

现有当前快照关系：

```text
information_item.current_version_no
→ information_snapshot(information_id, version_no)
```

Phase 3 Analysis 的历史身份直接绑定：

```text
information_analysis.snapshot_id
→ information_snapshot.id
```

Phase 3 增加内部 `snapshot_id` Reader/Mapper 查询即可，不增加 `current_snapshot_id`，不扩大公开 Job Query API，也不读取 `raw_payload`。

`FIRST_INGESTED` 使用 `information_item.first_seen_time`，候选稳定排序为：

```text
first_seen_time DESC, information_item.id DESC
```

## 5. Identity 与安全区域

Spring Security 过滤链区分两个认证区域：

```text
/api/v1/collector/**
    → 现有 Collector Bearer Token
    → 不使用用户 Session / CSRF

/api/v1/auth/**
/api/v1/jobs/**
/api/v1/ai/**
    → 浏览器 Session
    → 状态修改请求要求 CSRF
```

认证端点固定为：

```http
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/auth/me
GET  /api/v1/auth/csrf
```

第一版使用内存 Session，不创建 Spring Session JDBC 表。Web 与 Hub 保持同源，E2E 先登录后访问原有 Job 页面。

## 6. Prompt 与 Definition

```text
User
→ Prompt Profile
→ immutable Prompt Version
→ Definition Registry
   ├── Input Projection
   ├── System Prompt resource
   ├── Output Schema
   └── maxOutputTokens
→ Provider Request
```

用户只控制 User Prompt。平台控制 System Prompt、Definition Version、Projection 和 Schema。

Schedule 保存 Profile，不保存 Prompt Version；触发时解析 Active Version，然后把 Prompt Version 与当前 Definition Version冻结到 Batch。

## 7. Analysis Identity

逻辑唯一键：

```text
user
+ snapshot
+ prompt version
+ definition key/version
```

Provider/Model 是执行元数据，不属于业务身份。成功结果复用；失败显式重试复用同一 Analysis，并追加 Invocation。

## 8. Provider

Provider 抽象：

```text
AiProviderClient
├── FakeAiProviderClient
└── OpenAiCompatibleChatClient
```

OpenAI-compatible 实现复用 Spring Web 自带 `RestClient` 和现有 Jackson `ObjectMapper`，不引入 AI SDK。配置只在服务端，默认 disabled，CI 只使用 Fake。

真实网络调用必须在数据库事务之外。请求前先持久化 `RUNNING` Invocation，请求完成后在短事务内保存脱敏元数据、状态、结构化结果和 Provider Usage。

## 9. Actual Token

```text
Provider response usage
→ ai_model_invocation
→ Analysis / Batch / User 聚合
```

`ai_model_invocation` 是唯一 Actual Token 事实源。Estimate 仅用于 Preview 与 Budget。Invocation 绑定 `batch_item_id`，从而按 Batch 聚合时只统计该批真正产生的调用。

## 10. Manual Preview 与 Confirm

```text
Preview request
→ resolve absolute window
→ load ordered candidates
→ resolve current Snapshot
→ remove already-succeeded identity
→ estimate each candidate
→ apply candidate/token limits
→ return counts + HMAC preview token

Confirm
→ verify signature/expiry/owner/manualRequestId
→ recompute same absolute window and fingerprint
→ reject drift or create idempotent Batch + Items
```

Preview token 有效期 10 分钟，不建 Preview 表，不调用 Provider。Manual Batch 以 `(user_id, manual_request_id)` 防重复。

## 11. Batch 与 Worker

```text
Batch Creator
→ freeze Batch and ordered Items in one transaction
→ Worker polls
→ SELECT ... FOR UPDATE SKIP LOCKED
→ short claim transaction
→ provider call outside transaction
→ short completion transaction
```

第一版 Worker 单并发。任何 `RUNNING` 调用在崩溃后如果无法确认 Provider 是否执行，必须转为 `UNKNOWN`/失败，不自动重复收费。

Item-limit 之外只保留 Batch 聚合计数；Token-budget 延后项保存 Item 和明确原因，以支持审计和恢复。

## 12. Scheduler

```text
periodic dispatcher (30–60s)
→ lock due schedule
→ scheduledFor = nextRunAt
→ check overlap
→ create Scheduled or NOOP Batch idempotently
→ advance nextRunAt atomically
```

- due index：`(enabled, next_run_at, id)`；
- trigger unique：`(schedule_id, scheduled_for)`；
- 默认关闭、用户本地 02:00、IANA timezone；
- 5 分钟 misfire grace；
- 超过 grace 不补跑历史；
- Schedule 不直接调用 Provider。

## 13. 事务边界

必须使用明确事务：

- Profile 创建 Version 并切换 Active Version；
- 创建 Analysis 逻辑身份；
- Preview Confirm 创建 Batch/Items；
- Worker 领取；
- Worker 完成；
- Scheduler 触发并推进 `next_run_at`。

外部 HTTP 调用不得包含在数据库事务中。

## 14. 禁止的架构扩张

Phase 3 不增加：

- Kafka / RabbitMQ；
- Redis；
- Elasticsearch；
- Vector DB / Embedding / RAG / Agent；
- 微服务；
- Recommendation / Notification；
- Preview 持久化；
- Spring Session JDBC；
- 动态 Definition 或 System Prompt 数据库表。
