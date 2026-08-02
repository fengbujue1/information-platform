# Phase 3 Architecture Draft

状态：Proposed

## 1. 总体结构

```text
Browser
   │
   ├── Login
   ├── Prompt
   ├── Preview
   ├── Batch
   ├── Schedule
   └── Usage
   │
   ▼
Information Hub
   ├── identity
   ├── prompt
   ├── analysis
   │   ├── definition
   │   │   └── job
   │   ├── candidate
   │   │   └── job
   │   ├── batch
   │   ├── schedule
   │   └── provider
   ├── information
   └── job
        │
        ▼
      MySQL
        │
        ├── existing information tables
        └── Phase 3 AI tables
                │
                ▼
          AiProviderClient
                │
                ▼
       OpenAI-compatible Provider
```

## 2. 通用核心与领域实现

通用核心：

- User
- Prompt Profile
- Prompt Version
- Analysis Definition
- Information Analysis
- Model Invocation
- Batch
- Schedule
- Token Usage

JOB 专属：

- Job Input Projection
- Job Candidate Resolver
- Job User Relevance Output

不要为了未来信息类型创建巨大泛型框架。只抽取稳定边界。

## 3. Analysis Definition Registry

建议代码层存在：

```text
AnalysisDefinitionRegistry
└── JOB_USER_RELEVANCE_V1
```

Definition 提供：

- key/version；
- informationType；
- purpose；
- input projector；
- system prompt template；
- output schema；
- max output tokens；
- candidate resolver key。

未来新类型通过增加 Definition + Resolver 扩展。

## 4. Prompt Assembly

```text
Platform System Prompt
      +
Definition Rules
      +
User Prompt Version
      +
Structured Information Input
      ↓
Model
```

职位正文、标签等来源内容属于数据，不是指令。

平台必须通过清晰分隔和 System Instruction 防止来源文本覆盖分析规则。

## 5. Analysis Identity

默认逻辑幂等键：

```text
userId
+ snapshotId
+ promptVersionId
+ definitionKey
+ definitionVersion
```

已有 `SUCCEEDED`：

```text
REUSE / SKIP
```

Model / Provider 变化不自动触发重分析。

## 6. Provider

```text
Analysis Service
      ↓
AiProviderClient
      ↓
AiProviderResult<T>
```

统一结果至少包含：

- provider；
- model；
- provider request id；
- response；
- usage；
- latency；
- finish metadata。

每次真实请求形成一个 Model Invocation。

## 7. Actual Token

Actual Token 的事实来源：

```text
ai_model_invocation
```

不是 Preview，不是本地 Tokenizer。

即使 Analysis 后续 Schema Validation FAILED，只要 Provider 返回 Usage，也要保存。

## 8. Manual Batch

```text
Preview
→ Confirm
→ Create Batch
→ Freeze Candidates
→ Worker
→ Analyses
```

## 9. Scheduled Batch

```text
Schedule Due
→ Resolve Active Prompt Version
→ Internal Candidate Resolution
→ Apply Limits
→ Create Batch
→ Worker
```

之后与 Manual 完全共用。

## 10. Candidate Pipeline

第一版 JOB：

```text
FIRST_INGESTED window
→ eligibility
→ current snapshot
→ already analyzed skip
→ stable ordering
→ item limit
→ token budget
```

具体首次入库字段由 TASK-020 校正。

## 11. Token Budget Guard

至少检查：

- platform maxWindowDays；
- platform maxCandidates；
- requested limits；
- estimated token budget；
- per-call max output tokens。

执行中持续累计 Actual Usage。

Estimate 和 Actual 永远分开。

## 12. Scheduler

Phase 3 不引入 Quartz / XXL-JOB。

优先最小方案：

```text
Spring periodic dispatcher
→ query due schedules
→ DB lock / idempotency
→ create batch
```

最终实现由 TASK-030 结合项目现状确定。

## 13. Security Zones

建议：

```text
/api/v1/collector/**
→ existing Collector Bearer Token

/api/v1/auth/**
→ browser login / session

/api/v1/ai/**
→ authenticated user

/api/v1/jobs/**
→ TASK-020 决定是否纳入 session auth
```

Collector Token 和用户 Session 不混用。

## 14. Worker

第一版：

```text
MySQL persistent state
+ Spring background worker
```

要求：

- 重启可恢复；
- 不重复完成项；
- 不依赖浏览器连接；
- 低并发；
- 明确事务和锁；
- 不丢 Batch 状态。

## 15. Retry

不做所有错误的无脑自动重试。

原则：

- 明确未发送：可重试；
- 429：按 Provider backoff；
- 明确未执行：可重试；
- 请求已发出但客户端超时：结果不确定，MVP 默认不自动重试；
- Provider 已返回 Usage 但解析失败：记录 Usage，Analysis FAILED；
- Schema 无效：不得当成功保存。

## 16. 扩展方式

未来：

```text
analysis/definition/job
analysis/definition/news
analysis/definition/policy

analysis/candidate/job
analysis/candidate/news
analysis/candidate/policy
```

无需推翻 Prompt、Batch、Usage、Schedule 和 Provider。

## 17. 本阶段禁止

- Kafka
- RabbitMQ
- Redis Queue
- Elasticsearch
- Vector DB
- RAG
- Agent Framework
- 微服务
- Kubernetes

出现真实瓶颈后单独 ADR。
