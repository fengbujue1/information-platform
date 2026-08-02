# Information Platform Phase 3 数据库设计 Draft

设计版本：0.2 Draft  
状态：Proposed  
规划日期：2026-08-02  
数据库：MySQL 8.x  
字符集：utf8mb4  
时间存储：UTC

> 本文是 Phase 3 候选物理数据库设计，不是已经实施的数据库事实。
>
> 当前已实施数据库事实仍以 `docs/DATABASE_DESIGN.md` 和现有 Flyway migration 为准。
> 本文必须在 TASK-020 中与真实数据库、Flyway、PO、Mapper 和查询路径逐项核对。
> 用户确认后才可以转为 Accepted；TASK-024 才负责按 Accepted 设计实现新的 Flyway migration。

## 1. 文档职责

Phase 3 数据库相关文档分为三层：

```text
docs/DATABASE_DESIGN.md
    当前已经实施并 Accepted 的数据库事实

docs/PHASE3_DATA_MODEL_DRAFT.md
    Phase 3 领域模型、实体职责和关系原因

docs/DATABASE_DESIGN_PHASE3_DRAFT.md
    Phase 3 候选物理表结构、字段类型、索引、FK、幂等和删除规则
```

TASK-020 负责审查和冻结设计。

TASK-024 负责实现 Accepted 设计，不负责临场重新发明数据库结构。

如果 TASK-024 发现 Accepted 设计与真实代码/数据库无法兼容，必须停止实现，先修改设计文档并由用户确认。

## 2. 与当前数据库保持一致的约束

本 Draft 按当前 `DATABASE_DESIGN.md` 的既有约定设计：

1. MySQL 8.x。
2. `utf8mb4`。
3. 所有时间点统一按 UTC 存储。
4. 时间字段优先使用 `DATETIME(3)`。
5. 主业务 ID 延续 `BIGINT UNSIGNED AUTO_INCREMENT`。
6. 数据库不使用 MySQL `ENUM`，状态合法性由 Java 代码校验。
7. 数据库字段使用 `snake_case`。
8. Java 字段使用 `camelCase`。
9. 所有 schema 变更只通过新的 Flyway migration。
10. 已执行的旧 migration 不修改。
11. `information_snapshot` 是不可变历史事实。
12. `information_snapshot.information_id` 当前采用 `ON DELETE RESTRICT`。
13. AI 结果不能覆盖 `information_item`、`job_information` 或 `information_snapshot` 来源事实。

TASK-020 必须确认真实 migration 与本文建议完全一致；若不一致，以真实 migration 为事实，并修订本文。

## 3. Phase 3 候选新增表

Phase 3 候选新增 8 张表：

```text
user_account
ai_prompt_profile
ai_prompt_version
information_analysis
ai_model_invocation
ai_analysis_batch
ai_analysis_batch_item
ai_analysis_schedule
```

关系概览：

```text
user_account
    │
    ├─────────────────────────────┐
    │                             │
    ↓                             ↓
ai_prompt_profile          ai_analysis_schedule
    │                             │
    ↓                             │
ai_prompt_version                 │
    │                             │
    ├──────────────┐              │
    │              │              │
    ↓              ↓              ↓
information_analysis      ai_analysis_batch
    │                          │
    │                          ↓
    │                 ai_analysis_batch_item
    │                          │
    └──────────────┐           │
                   ↓           │
             ai_model_invocation

information_analysis
    │
    ├── information_item
    └── information_snapshot
```

## 4. 命名与边界

Phase 3 不建立：

```text
job_prompt
job_analysis
job_analysis_batch
```

原因：

- JOB 只是 Information 的一种类型；
- Prompt / Analysis / Batch / Schedule 是通用 AI Processing 能力；
- JOB 专属差异由 `analysis_definition_key`、`information_type` 和 Java Definition/Resolver 表达。

第一版真实业务仍然只有：

```text
information_type = JOB
analysis_definition_key = JOB_USER_RELEVANCE
analysis_definition_version = 1
analysis_purpose = USER_RELEVANCE
```

## 5. `user_account`

### 5.1 用途

保存 Phase 3 最小账号身份。

用于：

- 登录；
- Prompt Owner；
- Analysis Owner；
- Batch Owner；
- Schedule Owner；
- Token Usage 聚合；
- 用户时区。

不承担复杂 RBAC、多租户、组织和计费。

### 5.2 字段 Draft

| 字段 | 类型建议 | 允许为空 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `username` | VARCHAR(100) | 否 | - | 登录名 |
| `password_hash` | VARCHAR(255) | 否 | - | 安全密码摘要，绝不保存明文 |
| `display_name` | VARCHAR(255) | 是 | NULL | 页面显示名 |
| `timezone` | VARCHAR(64) | 否 | `UTC` 或 TASK-020 冻结值 | IANA Time Zone，例如 Asia/Shanghai |
| `status` | VARCHAR(32) | 否 | `ACTIVE` | ACTIVE / DISABLED 等，由 Java 校验 |
| `created_at` | DATETIME(3) | 否 | - | UTC |
| `updated_at` | DATETIME(3) | 否 | - | UTC |

### 5.3 约束与索引 Draft

```text
PRIMARY KEY (id)
UNIQUE (username)
INDEX (status)
```

### 5.4 删除规则

Phase 3 默认不提供用户硬删除。

原因：

- Prompt；
- Analysis；
- Invocation；
- Batch；
- Schedule；

都属于历史审计链。

账号停用优先使用 `status = DISABLED`。

### 5.5 TASK-020 必须确认

- username 是否大小写敏感；
- 是否需要数据库 collation 特殊处理；
- 初始账号如何 Bootstrap；
- timezone 默认值最终是什么；
- 是否需要 `last_login_at`，MVP 默认不需要。

## 6. `ai_prompt_profile`

### 6.1 用途

表示某个用户的一组长期 AI 关注点。

一个用户可以拥有多个 Prompt Profile。

例如：

```text
远程 Java 工作
Python / AI 项目
AI 行业新闻（未来）
```

Profile 绑定一个 Analysis Definition，不直接保存可变 Prompt 正文。

### 6.2 字段 Draft

| 字段 | 类型建议 | 允许为空 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT UNSIGNED | 否 | - | Owner |
| `name` | VARCHAR(255) | 否 | - | 用户可读名称 |
| `analysis_definition_key` | VARCHAR(128) | 否 | - | 如 JOB_USER_RELEVANCE |
| `active_version_id` | BIGINT UNSIGNED | 是 | NULL | 当前 Active Prompt Version |
| `status` | VARCHAR(32) | 否 | `ACTIVE` | ACTIVE / DISABLED |
| `created_at` | DATETIME(3) | 否 | - | UTC |
| `updated_at` | DATETIME(3) | 否 | - | UTC |

### 6.3 约束与索引 Draft

```text
PRIMARY KEY (id)
UNIQUE (user_id, name)
INDEX (user_id, status)
INDEX (analysis_definition_key)
```

FK：

```text
user_id
→ user_account.id
ON DELETE RESTRICT
```

`active_version_id` 的 FK 存在 Profile ↔ Version 循环依赖，TASK-024 可以：

1. 先创建两张表；
2. 再 `ALTER TABLE` 增加 FK；

候选规则：

```text
active_version_id
→ ai_prompt_version.id
ON DELETE RESTRICT
```

### 6.4 业务约束

- `active_version_id` 必须属于同一个 Profile；
- 数据库 FK 无法单独保证“Version 属于当前 Profile”，由 Service 事务校验；
- Profile 已被历史 Batch/Analysis 使用时不物理删除；
- 停用使用 `status`。

## 7. `ai_prompt_version`

### 7.1 用途

保存不可变的用户 Prompt 历史版本。

用户修改 Prompt 时：

```text
V1
→ create V2
→ Profile.active_version_id = V2
```

禁止：

```sql
UPDATE ai_prompt_version
SET content = ...
WHERE id = <历史版本>;
```

### 7.2 字段 Draft

| 字段 | 类型建议 | 允许为空 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `prompt_profile_id` | BIGINT UNSIGNED | 否 | - | 所属 Profile |
| `version_no` | INT UNSIGNED | 否 | - | 从 1 开始 |
| `content` | LONGTEXT | 否 | - | 用户 Prompt 原文 |
| `content_hash` | CHAR(64) | 否 | - | Prompt Canonical Text SHA-256 |
| `created_at` | DATETIME(3) | 否 | - | UTC |

### 7.3 约束与索引 Draft

```text
PRIMARY KEY (id)
UNIQUE (prompt_profile_id, version_no)
INDEX (prompt_profile_id, created_at)
INDEX (prompt_profile_id, content_hash)
```

FK：

```text
prompt_profile_id
→ ai_prompt_profile.id
ON DELETE RESTRICT
```

### 7.4 Version 规则

1. 同 Profile 的 `version_no` 单调递增。
2. 历史 Version 不更新。
3. 完全相同 Prompt 是否允许生成新 Version，由 TASK-022 冻结；建议检测 `content_hash` 后避免无意义重复。
4. 被 Analysis/Batch 使用后绝对不能物理删除。
5. Prompt 最大长度由 Java/API 校验，不依赖 LONGTEXT 容量作为业务上限。

## 8. `information_analysis`

### 8.1 用途

保存针对一个确定 Information Snapshot 的一次逻辑 AI 分析。

它是“业务分析结果”，不是底层 Provider Request。

### 8.2 逻辑身份

Phase 3 MVP 推荐唯一逻辑身份：

```text
user_id
+ snapshot_id
+ prompt_version_id
+ analysis_definition_key
+ analysis_definition_version
```

含义：

> 同一个用户、同一个信息快照、同一个 Prompt 版本、同一个分析定义版本，成功后普通重复调用必须复用结果。

Provider/Model 不属于默认逻辑身份。

### 8.3 字段 Draft

| 字段 | 类型建议 | 允许为空 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT UNSIGNED | 否 | - | Owner |
| `information_id` | BIGINT UNSIGNED | 否 | - | 冗余保存父 Information，便于查询与一致性校验 |
| `snapshot_id` | BIGINT UNSIGNED | 否 | - | 实际分析的不可变 Snapshot |
| `information_type` | VARCHAR(32) | 否 | - | 第一版 JOB |
| `analysis_definition_key` | VARCHAR(128) | 否 | - | JOB_USER_RELEVANCE |
| `analysis_definition_version` | INT UNSIGNED | 否 | - | 第一版 1 |
| `analysis_purpose` | VARCHAR(32) | 否 | - | USER_RELEVANCE |
| `prompt_profile_id` | BIGINT UNSIGNED | 否 | - | 便于 Owner/Profile 查询 |
| `prompt_version_id` | BIGINT UNSIGNED | 否 | - | 真正使用的不可变版本 |
| `status` | VARCHAR(32) | 否 | `PENDING` | PENDING/RUNNING/SUCCEEDED/FAILED |
| `result_json` | JSON | 是 | NULL | Schema 验证通过后的结构化结果 |
| `relevance_score` | TINYINT UNSIGNED | 是 | NULL | 0..100，由 Java 校验 |
| `summary` | VARCHAR(2000) | 是 | NULL | 页面常用摘要；完整结构仍以 result_json 为准 |
| `estimated_input_tokens` | BIGINT UNSIGNED | 是 | NULL | 分析前 Estimate |
| `estimated_output_tokens` | BIGINT UNSIGNED | 是 | NULL | 分析前 Estimate |
| `estimated_total_tokens` | BIGINT UNSIGNED | 是 | NULL | 分析前 Estimate |
| `estimate_method` | VARCHAR(64) | 是 | NULL | tokenizer/heuristic 等 |
| `started_at` | DATETIME(3) | 是 | NULL | UTC |
| `completed_at` | DATETIME(3) | 是 | NULL | UTC |
| `created_at` | DATETIME(3) | 否 | - | UTC |
| `updated_at` | DATETIME(3) | 否 | - | UTC |

### 8.4 约束与索引 Draft

```text
PRIMARY KEY (id)

UNIQUE (
  user_id,
  snapshot_id,
  prompt_version_id,
  analysis_definition_key,
  analysis_definition_version
)

INDEX (user_id, created_at)
INDEX (user_id, status, created_at)
INDEX (information_id, user_id)
INDEX (snapshot_id, user_id)
INDEX (prompt_profile_id, created_at)
INDEX (analysis_definition_key, analysis_definition_version)
```

FK：

```text
user_id
→ user_account.id
ON DELETE RESTRICT

information_id
→ information_item.id
ON DELETE RESTRICT

snapshot_id
→ information_snapshot.id
ON DELETE RESTRICT

prompt_profile_id
→ ai_prompt_profile.id
ON DELETE RESTRICT

prompt_version_id
→ ai_prompt_version.id
ON DELETE RESTRICT
```

### 8.5 一致性约束

Service 必须验证：

```text
snapshot.information_id == information_analysis.information_id
prompt_version.prompt_profile_id == information_analysis.prompt_profile_id
prompt_profile.user_id == information_analysis.user_id
```

MySQL 普通 FK 无法表达所有跨表组合一致性，必须通过 Application Service 事务保证。

### 8.6 成功后的重分析

Phase 3 MVP 默认：

- 相同逻辑身份已经 `SUCCEEDED` → 复用；
- Model/Provider 全局配置变化 → 不自动重跑；
- 如果确实需要重新产生业务结果，优先升级 Analysis Definition Version 或 Prompt Version。

这样 UNIQUE 可保持简单可靠。

如果未来必须支持“同 Definition/Prompt/Snapshot 下保存多次成功重分析历史”，再通过新 ADR 设计 `analysis_generation`，Phase 3 不提前加入。

## 9. `ai_model_invocation`

### 9.1 用途

保存每一次实际向模型 Provider 发出的请求结果元数据。

它是 Actual Token Usage 的事实来源。

例如：

```text
Analysis #100
├── Invocation #1  JSON 校验失败，但使用 4200 Token
└── Invocation #2  成功，使用 3900 Token

用户实际 Token = 8100
```

因此不能只在 `information_analysis` 上保存一个最终 Token 数。

### 9.2 字段 Draft

| 字段 | 类型建议 | 允许为空 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `analysis_id` | BIGINT UNSIGNED | 否 | - | 所属 Analysis |
| `user_id` | BIGINT UNSIGNED | 否 | - | 冗余 Owner，便于 Usage 聚合 |
| `provider` | VARCHAR(64) | 否 | - | OPENAI / OPENAI_COMPATIBLE 等 |
| `model_name` | VARCHAR(255) | 否 | - | 实际模型 |
| `provider_request_id` | VARCHAR(255) | 是 | NULL | Provider 返回的请求 ID |
| `attempt_no` | INT UNSIGNED | 否 | 1 | Analysis 内调用序号 |
| `status` | VARCHAR(32) | 否 | - | SUCCEEDED/FAILED/TIMEOUT/UNKNOWN |
| `input_tokens` | BIGINT UNSIGNED | 是 | NULL | Provider Actual Usage |
| `output_tokens` | BIGINT UNSIGNED | 是 | NULL | Provider Actual Usage |
| `total_tokens` | BIGINT UNSIGNED | 是 | NULL | Provider Actual Usage |
| `cached_input_tokens` | BIGINT UNSIGNED | 是 | NULL | Provider 可选 |
| `reasoning_tokens` | BIGINT UNSIGNED | 是 | NULL | Provider 可选 |
| `usage_status` | VARCHAR(32) | 否 | `UNAVAILABLE` | REPORTED/UNAVAILABLE |
| `latency_ms` | BIGINT UNSIGNED | 是 | NULL | 客户端观测耗时 |
| `error_code` | VARCHAR(128) | 是 | NULL | 安全错误码 |
| `error_message` | VARCHAR(2000) | 是 | NULL | 脱敏错误摘要 |
| `started_at` | DATETIME(3) | 否 | - | UTC |
| `completed_at` | DATETIME(3) | 是 | NULL | UTC |
| `created_at` | DATETIME(3) | 否 | - | UTC |

### 9.3 约束与索引 Draft

```text
PRIMARY KEY (id)
UNIQUE (analysis_id, attempt_no)

INDEX (user_id, created_at)
INDEX (analysis_id, created_at)
INDEX (provider, model_name, created_at)
INDEX (provider_request_id)
INDEX (usage_status, created_at)
```

FK：

```text
analysis_id
→ information_analysis.id
ON DELETE RESTRICT

user_id
→ user_account.id
ON DELETE RESTRICT
```

### 9.4 Token 规则

1. `input_tokens/output_tokens/total_tokens` 只有 Provider 报告时才写。
2. 本地 Estimated Token 禁止写入这些字段。
3. Provider 没有 Usage：
   ```text
   usage_status = UNAVAILABLE
   token fields = NULL
   ```
4. Provider 已返回 Usage，但后续 JSON / Schema Validation 失败：
   - Invocation Token 仍保留；
   - Analysis 可以 FAILED。
5. 客户端 timeout 且无法知道 Provider 是否完成：
   - `status = TIMEOUT/UNKNOWN`；
   - `usage_status = UNAVAILABLE`；
   - Phase 3 默认不无脑自动重试。

### 9.5 用户 Token 聚合

用户 Actual Usage 查询以此表聚合：

```sql
SUM(input_tokens)
SUM(output_tokens)
SUM(total_tokens)
WHERE user_id = ?
```

常用窗口：

```text
今日
本月
累计
指定 Batch
指定 Analysis
```

`NULL` 不等于 0；Usage 不可知必须显式保留不可知语义。

## 10. `ai_analysis_batch`

### 10.1 用途

表示一次：

- Manual Confirm；
- Scheduled Trigger；

创建的异步分析批次。

Batch 保存冻结后的 Prompt Version、时间窗口、限制和候选统计。

### 10.2 字段 Draft

| 字段 | 类型建议 | 允许为空 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT UNSIGNED | 否 | - | Owner |
| `trigger_type` | VARCHAR(32) | 否 | - | MANUAL / SCHEDULED |
| `schedule_id` | BIGINT UNSIGNED | 是 | NULL | Manual 为 NULL |
| `information_type` | VARCHAR(32) | 否 | - | 第一版 JOB |
| `analysis_definition_key` | VARCHAR(128) | 否 | - | Definition |
| `analysis_definition_version` | INT UNSIGNED | 否 | - | Definition Version |
| `prompt_profile_id` | BIGINT UNSIGNED | 否 | - | Profile |
| `prompt_version_id` | BIGINT UNSIGNED | 否 | - | Batch 创建时冻结 |
| `window_basis` | VARCHAR(32) | 否 | - | 第一版 FIRST_INGESTED |
| `requested_window_days` | INT UNSIGNED | 否 | - | 用户配置的 N |
| `window_start` | DATETIME(3) | 否 | - | 冻结绝对时间，UTC |
| `window_end` | DATETIME(3) | 否 | - | 冻结绝对时间，UTC |
| `requested_max_candidates` | INT UNSIGNED | 否 | - | 用户请求上限 |
| `requested_token_budget` | BIGINT UNSIGNED | 否 | - | Estimated Total Token Budget |
| `total_in_window` | INT UNSIGNED | 否 | 0 | 时间窗总数 |
| `eligible_count` | INT UNSIGNED | 否 | 0 | 业务 Eligible |
| `already_analyzed_count` | INT UNSIGNED | 否 | 0 | 当前逻辑身份已成功 |
| `selected_count` | INT UNSIGNED | 否 | 0 | 实际进入执行集 |
| `deferred_count` | INT UNSIGNED | 否 | 0 | 因 limit/budget 延期 |
| `estimated_input_tokens` | BIGINT UNSIGNED | 是 | NULL | Batch Estimate |
| `estimated_output_tokens` | BIGINT UNSIGNED | 是 | NULL | Batch Estimate |
| `estimated_total_tokens` | BIGINT UNSIGNED | 是 | NULL | Batch Estimate |
| `estimate_method` | VARCHAR(64) | 是 | NULL | Estimate 方法 |
| `status` | VARCHAR(32) | 否 | `PENDING` | PENDING/RUNNING/COMPLETED/PARTIAL_FAILED/FAILED/NOOP 等 |
| `skip_reason` | VARCHAR(64) | 是 | NULL | 无执行/跳过原因 |
| `scheduled_for` | DATETIME(3) | 是 | NULL | Scheduled 计划时间 UTC |
| `started_at` | DATETIME(3) | 是 | NULL | UTC |
| `completed_at` | DATETIME(3) | 是 | NULL | UTC |
| `created_at` | DATETIME(3) | 否 | - | UTC |
| `updated_at` | DATETIME(3) | 否 | - | UTC |

### 10.3 约束与索引 Draft

```text
PRIMARY KEY (id)

UNIQUE (schedule_id, scheduled_for)

INDEX (user_id, created_at)
INDEX (user_id, status, created_at)
INDEX (schedule_id, created_at)
INDEX (prompt_profile_id, created_at)
INDEX (window_start, window_end)
```

说明：

MySQL UNIQUE 允许多个 NULL，因此 Manual Batch：

```text
schedule_id = NULL
scheduled_for = NULL
```

不会互相冲突。

FK：

```text
user_id
→ user_account.id
ON DELETE RESTRICT

schedule_id
→ ai_analysis_schedule.id
ON DELETE RESTRICT

prompt_profile_id
→ ai_prompt_profile.id
ON DELETE RESTRICT

prompt_version_id
→ ai_prompt_version.id
ON DELETE RESTRICT
```

注意：`ai_analysis_schedule` 建表顺序与 Batch FK 存在依赖，TASK-024 可以先创建表，再通过 ALTER 添加 FK。

### 10.4 时间窗口语义

不能只保存：

```text
requested_window_days = 7
```

必须保存：

```text
window_start
window_end
```

因为 Batch 创建后候选集合必须冻结，不跟随“当前时间”继续移动。

第一版 `window_basis = FIRST_INGESTED`。

当前数据库已经有：

```text
information_item.first_seen_time
information_item.created_at
```

TASK-020 必须依据真实写入语义确认最终使用哪个字段；不能让 TASK-024 临时猜测。

## 11. `ai_analysis_batch_item`

### 11.1 用途

冻结 Batch 候选集合，并记录每个候选的选择和执行决策。

没有该表会导致：

- Batch 运行中数据变化；
- 无法解释为什么某条被跳过；
- Worker 重启后难以可靠恢复；
- 无法审计 limit/budget 截断。

### 11.2 字段 Draft

| 字段 | 类型建议 | 允许为空 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `batch_id` | BIGINT UNSIGNED | 否 | - | 所属 Batch |
| `information_id` | BIGINT UNSIGNED | 否 | - | Information |
| `snapshot_id` | BIGINT UNSIGNED | 否 | - | Batch 冻结时的 Snapshot |
| `analysis_id` | BIGINT UNSIGNED | 是 | NULL | 实际创建/复用的 Analysis |
| `selection_order` | INT UNSIGNED | 否 | - | 稳定候选顺序 |
| `status` | VARCHAR(32) | 否 | - | SELECTED/SUCCEEDED/FAILED/DEFERRED/SKIPPED 等 |
| `decision_reason` | VARCHAR(64) | 是 | NULL | ALREADY_ANALYZED/ITEM_LIMIT/TOKEN_BUDGET 等 |
| `estimated_input_tokens` | BIGINT UNSIGNED | 是 | NULL | Item Estimate |
| `estimated_output_tokens` | BIGINT UNSIGNED | 是 | NULL | Item Estimate |
| `estimated_total_tokens` | BIGINT UNSIGNED | 是 | NULL | Item Estimate |
| `created_at` | DATETIME(3) | 否 | - | UTC |
| `updated_at` | DATETIME(3) | 否 | - | UTC |

### 11.3 约束与索引 Draft

```text
PRIMARY KEY (id)
UNIQUE (batch_id, snapshot_id)

INDEX (batch_id, selection_order)
INDEX (batch_id, status)
INDEX (analysis_id)
INDEX (snapshot_id)
```

FK：

```text
batch_id
→ ai_analysis_batch.id
ON DELETE RESTRICT

information_id
→ information_item.id
ON DELETE RESTRICT

snapshot_id
→ information_snapshot.id
ON DELETE RESTRICT

analysis_id
→ information_analysis.id
ON DELETE RESTRICT
```

### 11.4 候选稳定排序

JOB 第一版候选排序必须显式定义。

建议候选：

```text
first_seen_time DESC
information_item.id DESC
```

最终由 TASK-020/TASK-028 根据真实查询字段冻结。

数据库不能依赖自然顺序。

## 12. `ai_analysis_schedule`

### 12.1 用途

保存用户配置的每日自动分析规则。

Schedule 自己不调用模型，只负责到点创建一个 Scheduled Batch。

### 12.2 字段 Draft

| 字段 | 类型建议 | 允许为空 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT UNSIGNED | 否 | - | Owner |
| `name` | VARCHAR(255) | 否 | - | 页面名称 |
| `information_type` | VARCHAR(32) | 否 | - | 第一版 JOB |
| `analysis_definition_key` | VARCHAR(128) | 否 | - | Definition |
| `prompt_profile_id` | BIGINT UNSIGNED | 否 | - | 跟随 Profile Active Version |
| `enabled` | TINYINT(1) | 否 | `0` | 默认关闭 |
| `local_time` | TIME | 否 | `02:00:00` | 用户墙上时间 |
| `timezone` | VARCHAR(64) | 否 | - | IANA timezone |
| `window_days` | INT UNSIGNED | 否 | - | 最近 N 天 |
| `max_candidates` | INT UNSIGNED | 否 | - | 单次候选上限 |
| `max_estimated_tokens` | BIGINT UNSIGNED | 否 | - | 单次 Estimated Token Budget |
| `last_triggered_at` | DATETIME(3) | 是 | NULL | 最近成功创建/处理触发时间 |
| `next_run_at` | DATETIME(3) | 是 | NULL | 下一次 UTC 计划点 |
| `created_at` | DATETIME(3) | 否 | - | UTC |
| `updated_at` | DATETIME(3) | 否 | - | UTC |

### 12.3 约束与索引 Draft

```text
PRIMARY KEY (id)
UNIQUE (user_id, name)

INDEX (enabled, next_run_at)
INDEX (user_id, enabled)
INDEX (prompt_profile_id)
```

FK：

```text
user_id
→ user_account.id
ON DELETE RESTRICT

prompt_profile_id
→ ai_prompt_profile.id
ON DELETE RESTRICT
```

### 12.4 默认规则

```text
enabled = 0
local_time = 02:00:00
```

timezone 不建议直接假定服务器时区。

用户创建/修改 Schedule 时必须保存明确 IANA timezone。

### 12.5 Prompt Version

Schedule 不保存固定 `prompt_version_id`。

它保存：

```text
prompt_profile_id
```

到点时：

```text
resolve active_version_id
→ create Batch
→ batch.prompt_version_id = resolved version
```

Batch 一旦创建，Prompt Version 不再变化。

### 12.6 Overlap

同一个 Schedule 同时最多一个活动 Batch。

到点时存在：

```text
PENDING
RUNNING
```

Batch：

```text
SKIP_CONCURRENT_RUN
```

具体如何记录“本次计划被跳过”由 TASK-030 冻结：

- 生成 `NOOP/SKIPPED` Batch；
- 或独立 schedule run 记录。

Phase 3 Draft 当前优先复用 Batch，不增加第 9 张表。

### 12.7 Misfire

服务器停机错过执行时间：

```text
SKIP
```

不自动补跑历史所有遗漏。

`next_run_at` 向下一个未来计划点推进。

## 13. Schedule 触发幂等

候选幂等：

```text
UNIQUE (schedule_id, scheduled_for)
```

放在 `ai_analysis_batch`。

目的：

即使 Dispatcher：

- 重复扫描；
- 重启；
- 事务重试；
- 多次进入相同逻辑；

同一个 Schedule 的同一计划时间也不能创建两个费用 Batch。

TASK-030 必须对数据库唯一冲突做幂等处理，而不是当系统故障无限重试。

## 14. Batch 与 Schedule 的建表依赖

存在互相引用：

```text
ai_analysis_batch.schedule_id
→ ai_analysis_schedule.id

ai_analysis_schedule.prompt_profile_id
→ ai_prompt_profile.id
```

建议 migration 顺序：

```text
1. user_account
2. ai_prompt_profile（active_version_id FK 暂后）
3. ai_prompt_version
4. 给 ai_prompt_profile 增加 active_version_id FK
5. information_analysis
6. ai_model_invocation
7. ai_analysis_schedule
8. ai_analysis_batch
9. ai_analysis_batch_item
```

如果 Migration 文件仍采用单个 V2，可在文件中按上述顺序执行。

TASK-024 必须根据真实 Flyway 命名和现有 migration 数量决定实际版本号，不能假定一定是 V2。

## 15. 状态字段

延续当前数据库“不使用 ENUM”的原则。

建议 Java 枚举：

### user_account.status

```text
ACTIVE
DISABLED
```

### ai_prompt_profile.status

```text
ACTIVE
DISABLED
```

### information_analysis.status

```text
PENDING
RUNNING
SUCCEEDED
FAILED
```

### ai_model_invocation.status

```text
SUCCEEDED
FAILED
TIMEOUT
UNKNOWN
```

### ai_model_invocation.usage_status

```text
REPORTED
UNAVAILABLE
```

### ai_analysis_batch.trigger_type

```text
MANUAL
SCHEDULED
```

### ai_analysis_batch.status

候选：

```text
PENDING
RUNNING
COMPLETED
PARTIAL_FAILED
FAILED
NOOP
```

### ai_analysis_batch_item.status

候选：

```text
SELECTED
RUNNING
SUCCEEDED
FAILED
SKIPPED
DEFERRED
```

最终枚举必须在对应 Contract/TASK 中冻结，不能因为数据库是 VARCHAR 就任意写字符串。

## 16. Analysis Definition 是否建表

Phase 3 Draft 决定：

> `analysis_definition` 第一版不建数据库表。

原因：

- Definition 是代码与 Contract 共同维护的平台协议；
- 包含 Input Projection、System Prompt Template、Schema Validator 等代码行为；
- 第一版只有 `JOB_USER_RELEVANCE_V1`；
- 用数据库动态配置反而会制造“DB 配置和代码 Validator 不一致”的风险。

因此数据库只保存：

```text
analysis_definition_key
analysis_definition_version
```

用于历史追溯。

未来如果真的需要后台动态发布 Definition，再单独 ADR。

## 17. System Prompt 是否建表

Phase 3 Draft 决定：

> 第一版 System Prompt 不建用户可编辑数据库表。

System Prompt 与 Definition Version 一起作为代码/资源版本管理。

历史 Analysis 保存：

```text
analysis_definition_key
analysis_definition_version
```

即可定位对应平台 Prompt 规则。

如果未来需要独立 System Prompt 发布历史，再新增版本表，不在 Phase 3 预先建设。

## 18. Actual Token 事实来源

唯一事实来源：

```text
ai_model_invocation
```

下列字段都是派生或 Estimate，不可作为 Actual 事实：

```text
information_analysis.estimated_*
ai_analysis_batch.estimated_*
ai_analysis_batch_item.estimated_*
```

用户 Usage：

```text
SUM(ai_model_invocation.total_tokens)
```

Batch Usage：

```text
Batch
→ Batch Item
→ Analysis
→ Invocation
→ SUM(total_tokens)
```

如果未来性能证明 Join 聚合昂贵，可以增加汇总缓存，但缓存不能成为独立事实源。

## 19. Cost 字段

Phase 3 核心验收要求是 Token，而不是 SaaS Billing。

因此本 Draft 默认不强制在 8 张表中加入金额字段。

如果 TASK-020 确认 Phase 3 必须展示费用：

建议再冻结以下概念：

```text
pricing_snapshot
currency
estimated_cost
derived_actual_cost
```

并明确：

- Cost 是由 Token × 当时价格推导；
- Token 是 Provider Usage 事实；
- 不能用今天价格重算历史后称为“实际账单”。

如果没有可靠价格快照，费用字段保持不存在或 NULL。

## 20. Session 是否建表

Identity MVP 推荐浏览器 Session。

本 Draft 不默认创建：

```text
user_session
```

是否使用：

- Spring 内存 Session；
- Spring Session JDBC；

由 TASK-020/TASK-021 根据部署可靠性决定。

如果使用 JDBC Session，应优先采用 Spring Session 官方 schema，而不是自行设计认证 Session 表。

## 21. Preview 是否建表

Manual Preview 第一版不默认建 `ai_analysis_preview` 表。

Preview 是：

```text
read + estimate
```

但 Manual Confirm 必须确保使用相同冻结窗口。

可选方案：

1. 前端带回 server-signed Preview Token；
2. 服务端短期缓存；
3. 创建轻量 Preview Record。

TASK-020/TASK-028 冻结最终方案。

只有确有必要才新增第 9 张业务表。

## 22. FIRST_INGESTED 时间字段

用户需求语义：

> 最近 N 天首次入库的信息。

当前 Accepted 数据库同时存在：

```text
information_item.first_seen_time
information_item.created_at
```

从字段语义上，`first_seen_time` 更接近“服务端首次发现时间”，但最终必须检查：

- 当前 Ingestion Service 实际赋值；
- 测试；
- 历史导入；
- 是否与 created_at 存在差异。

因此本 Draft 不把 SQL 写死。

TASK-020 必须输出：

```text
window_basis = FIRST_INGESTED
physical_column = <confirmed field>
```

之后 TASK-028 才实现查询。

## 23. 关键查询与索引审查清单

TASK-020 / TASK-024 必须从真实查询反推索引。

至少验证：

### Prompt

```text
current user's profiles
profile version history
active version
```

### Analysis

```text
logical idempotency lookup
user + information
user + snapshot
recent user analyses
```

### Usage

```text
user + today
user + current month
user + all time
analysis invocations
```

### Batch

```text
user recent batches
batch status
schedule recent batches
batch items by selection order
```

### Scheduler

高频扫描：

```text
enabled = 1
AND next_run_at <= now
```

因此至少需要：

```text
INDEX (enabled, next_run_at)
```

## 24. 删除策略汇总 Draft

| 表 | 建议删除策略 |
|---|---|
| `user_account` | 不物理删除；DISABLED |
| `ai_prompt_profile` | 不物理删除；DISABLED |
| `ai_prompt_version` | RESTRICT，不可修改历史 |
| `information_analysis` | 历史审计优先保留 |
| `ai_model_invocation` | 历史 Token 事实，RESTRICT |
| `ai_analysis_batch` | 历史审计优先保留 |
| `ai_analysis_batch_item` | 与 Batch 一起保留 |
| `ai_analysis_schedule` | 优先 enabled=false |

Phase 3 不设计用户“一键物理删除全部历史”能力。

## 25. 数据安全

数据库禁止保存：

- AI Provider API Key；
- Authorization Header；
- 浏览器 Cookie；
- Collector Token；
- 用户明文密码。

`error_message` 必须保存脱敏摘要，不保存完整 HTTP Header/Request。

Prompt 内容属于用户数据，查询必须做 Owner 隔离。

## 26. Migration 原则

TASK-024：

1. 读取现有 migration 文件名和最新版本。
2. 新建下一个 migration。
3. 不修改 V1/历史 migration。
4. 空测试库全量 migrate 必须成功。
5. 已有共享开发库 migrate 必须成功。
6. Migration 必须可重复验证，但不要求 down migration。
7. Flyway clean 保持禁用。
8. migration 与 `DATABASE_DESIGN.md` 最终同步。

## 27. TASK-020 数据库专项审查

Codex 在 TASK-020 必须逐项回答：

1. 是否真的需要这 8 张表。
2. 现有 `information_item.id` 和 `information_snapshot.id` 是否确实为 `BIGINT UNSIGNED`。
3. `snapshot_id` FK 是否可直接建立。
4. `first_seen_time` / `created_at` 哪个符合 FIRST_INGESTED。
5. Prompt Profile ↔ Version 的 Active FK 是否值得保留。
6. `UNIQUE (user_id, name)` 是否符合 Prompt/Schedule UX。
7. Analysis UNIQUE 是否与失败重试设计冲突。
8. Model Invocation 是否足以覆盖多次调用 Token。
9. Batch Item 是否需要冻结所有候选，还是只冻结 Selected；本 Draft 推荐保存需要审计的候选决策。
10. `UNIQUE(schedule_id, scheduled_for)` 在 MySQL 中是否满足 Manual NULL 语义。
11. Scheduler 高频查询索引是否足够。
12. 所有 FK 的 ON DELETE 是否与既有历史保护规则一致。
13. `VARCHAR/TEXT/JSON/DATETIME(3)` 是否与现有工程约定一致。
14. 是否有任何字段其实属于代码常量，不应入库。
15. 是否有缺失的真实查询索引。
16. 是否存在不必要的提前设计字段。
17. migration 实际版本号应该是什么。
18. 是否需要 Spring Session JDBC 表。
19. 是否需要 Preview 持久化表。
20. 用户确认前不得创建 SQL。

## 28. TASK-024 实施纪律

TASK-024 输入必须是：

```text
Accepted docs/PHASE3_DATA_MODEL_DRAFT.md
+
Accepted docs/DATABASE_DESIGN_PHASE3_DRAFT.md
+
current docs/DATABASE_DESIGN.md
+
current Flyway migrations
```

Codex 不得在 TASK-024 自行改变：

- 表数量；
- 字段语义；
- FK；
- UNIQUE；
- 核心索引；
- Token 事实来源；
- Schedule 幂等策略。

如发现 Accepted 设计有问题：

```text
STOP
→ 汇报问题
→ 修订设计
→ 用户确认
→ 再继续 migration
```

## 29. TASK-024 完成后的文档归并

TASK-024 完成并通过数据库测试后：

1. `DATABASE_DESIGN_PHASE3_DRAFT.md` 作为设计历史可以保留并改名/标记 Accepted，或按仓库文档规范继续保留。
2. 真实已实施 Phase 3 表结构必须合并进：
   ```text
   docs/DATABASE_DESIGN.md
   ```
3. `DATABASE_DESIGN.md` 的设计版本提升。
4. 文档记录真实 Flyway migration 文件名。
5. 文档与 SQL 不一致时，以实际 migration 为事实并立即修正文档。
