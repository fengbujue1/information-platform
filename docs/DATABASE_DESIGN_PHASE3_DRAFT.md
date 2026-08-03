# Information Platform Phase 3 数据库设计

设计版本：1.0 Accepted
状态：Accepted
接受日期：2026-08-03
数据库：MySQL 8.x  
字符集：utf8mb4  
时间存储：UTC

> 文件名保留 `_DRAFT` 以维持既有链接。本文已经 TASK-020 真实仓库审查并由用户确认，是 TASK-024 的物理数据库设计输入，但不是“已经实施”的数据库事实。
>
> 当前已实施事实仍以 `docs/DATABASE_DESIGN.md` 和现有 Flyway migration 为准。TASK-024 完成后才把真实 migration 合并到 `DATABASE_DESIGN.md`。

## 1. 已核对的现有数据库事实

真实 Flyway 只有：

```text
backend/information-hub/src/main/resources/db/migration/
└── V1__create_information_job_and_snapshot_tables.sql
```

与 Phase 3 FK 相关的真实事实：

| 项目 | 真实结构 |
|---|---|
| `information_item.id` | `BIGINT UNSIGNED AUTO_INCREMENT` 主键 |
| `information_snapshot.id` | `BIGINT UNSIGNED AUTO_INCREMENT` 主键 |
| Snapshot 业务唯一键 | `(information_id, version_no)` |
| Snapshot 父 FK | `information_snapshot.information_id → information_item.id ON DELETE RESTRICT` |
| 当前 Snapshot | `information_item.current_version_no → (information_id, version_no)`，应用查询关联 |
| 首次进入平台时间 | `information_item.first_seen_time`，首次插入由服务端设置，后续不变 |

Phase 3 不新增 `current_snapshot_id`，不修改 V1。

## 2. 通用物理规则

1. ID 和 FK 使用 `BIGINT UNSIGNED`。
2. 时间点使用 UTC `DATETIME(3)`；墙上时间使用 `TIME` 并配 IANA timezone。
3. `created_at` 默认 `CURRENT_TIMESTAMP(3)`。
4. 可变配置/状态表的 `updated_at` 默认 `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)`。
5. 不使用 MySQL `ENUM`，状态由 Java enum 和 Application Service 校验。
6. 历史链全部 `ON DELETE RESTRICT ON UPDATE RESTRICT`。
7. 不硬删除 AI 历史记录；账号、Profile、Schedule 以状态停用。
8. 所有 schema 变更只通过新的 Flyway migration；历史 migration 不修改。
9. API Key、Authorization、Cookie、Collector Token 和明文密码禁止入库。
10. Provider 错误只保存脱敏摘要，不保存完整原始响应。

## 3. Phase 3 新增表

必须新增且仅新增以下 8 张业务表：

```text
user_account
ai_prompt_profile
ai_prompt_version
information_analysis
ai_analysis_schedule
ai_analysis_batch
ai_analysis_batch_item
ai_model_invocation
```

第一版不创建 Spring Session JDBC、Preview、Schedule Run、Definition、System Prompt、Usage Summary 或 Cost 表。

## 4. 现有表新增索引

TASK-024 在新 migration 中为 FIRST_INGESTED 候选查询增加：

```text
information_item:
INDEX idx_information_item_type_first_seen_id
      (information_type, first_seen_time, id)
```

查询固定为：

```sql
WHERE information_type = 'JOB'
  AND first_seen_time >= :window_start
  AND first_seen_time < :window_end
ORDER BY first_seen_time DESC, id DESC
```

不修改 V1 文件。

## 5. `user_account`

### 5.1 字段

| 字段 | 类型 | NULL | 默认 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `username` | VARCHAR(100) | 否 | - | trim/lower 后的登录名 |
| `password_hash` | VARCHAR(255) | 否 | - | 安全密码摘要 |
| `display_name` | VARCHAR(255) | 是 | NULL | 页面显示名 |
| `timezone` | VARCHAR(64) | 否 | `Asia/Shanghai` | IANA timezone |
| `status` | VARCHAR(32) | 否 | `ACTIVE` | ACTIVE/DISABLED |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | UTC |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3), ON UPDATE | UTC |

### 5.2 约束与索引

```text
PRIMARY KEY (id)
UNIQUE KEY uk_user_account_username (username)
```

数据库使用当前 `utf8mb4_0900_ai_ci` 的大小写不敏感比较，应用仍必须执行 `trim + lower-case`。不为低选择性的 `status` 单独建索引。

## 6. `ai_prompt_profile`

### 6.1 字段

| 字段 | 类型 | NULL | 默认 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT UNSIGNED | 否 | - | Owner |
| `name` | VARCHAR(255) | 否 | - | 用户内唯一名称 |
| `analysis_definition_key` | VARCHAR(128) | 否 | - | 第一版 JOB_USER_RELEVANCE |
| `active_version_id` | BIGINT UNSIGNED | 是 | NULL | 当前 Prompt Version |
| `status` | VARCHAR(32) | 否 | `ACTIVE` | ACTIVE/DISABLED |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | UTC |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3), ON UPDATE | UTC |

### 6.2 约束、索引与 FK

```text
PRIMARY KEY (id)
UNIQUE KEY uk_ai_prompt_profile_user_name (user_id, name)
KEY idx_ai_prompt_profile_user_status (user_id, status)

user_id → user_account.id
ON DELETE RESTRICT ON UPDATE RESTRICT

active_version_id → ai_prompt_version.id
ON DELETE RESTRICT ON UPDATE RESTRICT
```

`active_version_id` FK 在 Version 建表后用 `ALTER TABLE` 增加。数据库 FK 不能保证 Version 属于同一 Profile，Service 必须在切换事务中校验。

## 7. `ai_prompt_version`

### 7.1 字段

| 字段 | 类型 | NULL | 默认 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `prompt_profile_id` | BIGINT UNSIGNED | 否 | - | 所属 Profile |
| `version_no` | INT UNSIGNED | 否 | - | 从 1 开始 |
| `content` | LONGTEXT | 否 | - | 不可变 User Prompt |
| `content_hash` | CHAR(64) | 否 | - | Canonical UTF-8 SHA-256 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | UTC |

### 7.2 约束、索引与 FK

```text
PRIMARY KEY (id)
UNIQUE KEY uk_ai_prompt_version_no
           (prompt_profile_id, version_no)
UNIQUE KEY uk_ai_prompt_version_content
           (prompt_profile_id, content_hash)
KEY idx_ai_prompt_version_profile_created
    (prompt_profile_id, created_at)

prompt_profile_id → ai_prompt_profile.id
ON DELETE RESTRICT ON UPDATE RESTRICT
```

相同 Profile 的相同 `content_hash` 复用已有 Version。正文不可更新，8,000 字符业务上限由 Java 校验。

## 8. `information_analysis`

### 8.1 字段

| 字段 | 类型 | NULL | 默认 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT UNSIGNED | 否 | - | Owner |
| `information_id` | BIGINT UNSIGNED | 否 | - | 父 Information |
| `snapshot_id` | BIGINT UNSIGNED | 否 | - | 实际分析的 Snapshot |
| `information_type` | VARCHAR(32) | 否 | - | 第一版 JOB |
| `analysis_definition_key` | VARCHAR(128) | 否 | - | JOB_USER_RELEVANCE |
| `analysis_definition_version` | INT UNSIGNED | 否 | - | 第一版 1 |
| `analysis_purpose` | VARCHAR(32) | 否 | - | USER_RELEVANCE |
| `prompt_profile_id` | BIGINT UNSIGNED | 否 | - | Profile |
| `prompt_version_id` | BIGINT UNSIGNED | 否 | - | 不可变 Prompt Version |
| `status` | VARCHAR(32) | 否 | `PENDING` | PENDING/RUNNING/SUCCEEDED/FAILED |
| `result_json` | JSON | 是 | NULL | Schema 校验通过的结果 |
| `relevance_score` | TINYINT UNSIGNED | 是 | NULL | 0..100 |
| `summary` | VARCHAR(2000) | 是 | NULL | 页面投影 |
| `estimated_input_tokens` | BIGINT UNSIGNED | 是 | NULL | Estimate |
| `estimated_output_tokens` | BIGINT UNSIGNED | 是 | NULL | Estimate |
| `estimated_total_tokens` | BIGINT UNSIGNED | 是 | NULL | Estimate |
| `estimate_method` | VARCHAR(64) | 是 | NULL | 算法版本 |
| `failure_code` | VARCHAR(128) | 是 | NULL | 稳定失败码 |
| `failure_message` | VARCHAR(2000) | 是 | NULL | 脱敏失败摘要 |
| `started_at` | DATETIME(3) | 是 | NULL | UTC |
| `completed_at` | DATETIME(3) | 是 | NULL | UTC |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | UTC |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3), ON UPDATE | UTC |

### 8.2 约束、索引与 FK

```text
PRIMARY KEY (id)
UNIQUE KEY uk_information_analysis_identity (
  user_id,
  snapshot_id,
  prompt_version_id,
  analysis_definition_key,
  analysis_definition_version
)
KEY idx_information_analysis_user_created (user_id, created_at)
KEY idx_information_analysis_user_status_created (user_id, status, created_at)
KEY idx_information_analysis_information_user (information_id, user_id)
KEY idx_information_analysis_snapshot_user (snapshot_id, user_id)
KEY idx_information_analysis_profile_created (prompt_profile_id, created_at)

user_id           → user_account.id          RESTRICT
information_id    → information_item.id      RESTRICT
snapshot_id       → information_snapshot.id  RESTRICT
prompt_profile_id → ai_prompt_profile.id     RESTRICT
prompt_version_id → ai_prompt_version.id     RESTRICT
```

所有 FK 都是 `ON DELETE RESTRICT ON UPDATE RESTRICT`。

Service 事务必须验证：

```text
snapshot.information_id = information_id
prompt_version.prompt_profile_id = prompt_profile_id
prompt_profile.user_id = user_id
```

唯一键对所有状态生效。失败重试更新同一 Analysis 状态并追加 Invocation；成功重复调用复用。

## 9. `ai_analysis_schedule`

### 9.1 字段

| 字段 | 类型 | NULL | 默认 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT UNSIGNED | 否 | - | Owner |
| `name` | VARCHAR(255) | 否 | - | 用户内唯一名称 |
| `prompt_profile_id` | BIGINT UNSIGNED | 否 | - | 到点解析 Active Version |
| `enabled` | TINYINT(1) | 否 | 0 | 默认关闭 |
| `local_time` | TIME | 否 | `02:00:00` | 用户墙上时间 |
| `timezone` | VARCHAR(64) | 否 | `Asia/Shanghai` | IANA timezone |
| `window_days` | INT UNSIGNED | 否 | 3 | 1..14 |
| `max_candidates` | INT UNSIGNED | 否 | 20 | 1..50 |
| `max_estimated_tokens` | BIGINT UNSIGNED | 否 | 75000 | 1..200000 |
| `next_run_at` | DATETIME(3) | 是 | NULL | enabled 时的下一 UTC 计划点 |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | UTC |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3), ON UPDATE | UTC |

### 9.2 约束、索引与 FK

```text
PRIMARY KEY (id)
UNIQUE KEY uk_ai_analysis_schedule_user_name (user_id, name)
KEY idx_ai_analysis_schedule_due (enabled, next_run_at, id)
KEY idx_ai_analysis_schedule_user_enabled (user_id, enabled)
KEY idx_ai_analysis_schedule_profile (prompt_profile_id)

user_id           → user_account.id      RESTRICT
prompt_profile_id → ai_prompt_profile.id RESTRICT
```

所有 FK 都是 `ON DELETE RESTRICT ON UPDATE RESTRICT`。

不保存 `information_type`、`analysis_definition_key` 或 `last_triggered_at`：前两者由 Profile/Definition Registry 解析并冻结到 Batch，最近运行从 Batch 查询。`enabled=0` 时 `next_run_at` 必须为 `NULL`。

## 10. `ai_analysis_batch`

### 10.1 字段

| 字段 | 类型 | NULL | 默认 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT UNSIGNED | 否 | - | Owner |
| `trigger_type` | VARCHAR(32) | 否 | - | MANUAL/SCHEDULED |
| `manual_request_id` | CHAR(36) | 是 | NULL | Manual Confirm 幂等 UUID |
| `schedule_id` | BIGINT UNSIGNED | 是 | NULL | Scheduled 对应 Schedule |
| `scheduled_for` | DATETIME(3) | 是 | NULL | Scheduled 计划点 |
| `information_type` | VARCHAR(32) | 否 | - | 第一版 JOB |
| `analysis_definition_key` | VARCHAR(128) | 否 | - | 冻结 Definition |
| `analysis_definition_version` | INT UNSIGNED | 否 | - | 冻结 Definition Version |
| `prompt_profile_id` | BIGINT UNSIGNED | 否 | - | Profile |
| `prompt_version_id` | BIGINT UNSIGNED | 否 | - | 冻结 Active Version |
| `window_basis` | VARCHAR(32) | 否 | `FIRST_INGESTED` | 时间语义 |
| `requested_window_days` | INT UNSIGNED | 否 | - | 请求 N 天 |
| `window_start` | DATETIME(3) | 否 | - | UTC，包含 |
| `window_end` | DATETIME(3) | 否 | - | UTC，不包含 |
| `requested_max_candidates` | INT UNSIGNED | 否 | - | 请求候选上限 |
| `requested_token_budget` | BIGINT UNSIGNED | 否 | - | Estimated Total Budget |
| `total_in_window` | INT UNSIGNED | 否 | 0 | 窗口总数 |
| `eligible_count` | INT UNSIGNED | 否 | 0 | 业务可分析数 |
| `already_analyzed_count` | INT UNSIGNED | 否 | 0 | 已成功复用数 |
| `selected_count` | INT UNSIGNED | 否 | 0 | 实际执行集 |
| `deferred_by_item_limit_count` | INT UNSIGNED | 否 | 0 | 超数量限制 |
| `deferred_by_token_budget_count` | INT UNSIGNED | 否 | 0 | 超 Token Budget |
| `estimated_input_tokens` | BIGINT UNSIGNED | 是 | NULL | Selected Estimate |
| `estimated_output_tokens` | BIGINT UNSIGNED | 是 | NULL | Selected Estimate |
| `estimated_total_tokens` | BIGINT UNSIGNED | 是 | NULL | Selected Estimate |
| `estimate_method` | VARCHAR(64) | 是 | NULL | 算法版本 |
| `status` | VARCHAR(32) | 否 | `PENDING` | Batch 状态 |
| `skip_reason` | VARCHAR(64) | 是 | NULL | NOOP 原因 |
| `started_at` | DATETIME(3) | 是 | NULL | UTC |
| `completed_at` | DATETIME(3) | 是 | NULL | UTC |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | UTC |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3), ON UPDATE | UTC |

### 10.2 约束、索引与 FK

```text
PRIMARY KEY (id)
UNIQUE KEY uk_ai_analysis_batch_manual (user_id, manual_request_id)
UNIQUE KEY uk_ai_analysis_batch_schedule (schedule_id, scheduled_for)
KEY idx_ai_analysis_batch_user_created (user_id, created_at)
KEY idx_ai_analysis_batch_user_status_created (user_id, status, created_at)
KEY idx_ai_analysis_batch_schedule_status (schedule_id, status)
KEY idx_ai_analysis_batch_profile_created (prompt_profile_id, created_at)

user_id           → user_account.id          RESTRICT
schedule_id       → ai_analysis_schedule.id  RESTRICT
prompt_profile_id → ai_prompt_profile.id     RESTRICT
prompt_version_id → ai_prompt_version.id     RESTRICT
```

所有 FK 都是 `ON DELETE RESTRICT ON UPDATE RESTRICT`。

trigger 组合约束：

```text
MANUAL:
  manual_request_id IS NOT NULL
  schedule_id IS NULL
  scheduled_for IS NULL

SCHEDULED:
  manual_request_id IS NULL
  schedule_id IS NOT NULL
  scheduled_for IS NOT NULL
```

TASK-024 使用 MySQL 8 `CHECK` 表达可表达部分，Application Service 仍需校验。MySQL UNIQUE 允许多个 NULL，Manual Batch 不会被 Scheduled 唯一键互相冲突。

## 11. `ai_analysis_batch_item`

### 11.1 字段

| 字段 | 类型 | NULL | 默认 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `batch_id` | BIGINT UNSIGNED | 否 | - | 所属 Batch |
| `information_id` | BIGINT UNSIGNED | 否 | - | Information |
| `snapshot_id` | BIGINT UNSIGNED | 否 | - | 冻结 Snapshot |
| `analysis_id` | BIGINT UNSIGNED | 是 | NULL | 创建或复用的 Analysis |
| `selection_order` | INT UNSIGNED | 否 | - | 从 1 开始的稳定顺序 |
| `status` | VARCHAR(32) | 否 | `SELECTED` | Item 状态 |
| `decision_reason` | VARCHAR(64) | 是 | NULL | ALREADY_ANALYZED/TOKEN_BUDGET 等 |
| `estimated_input_tokens` | BIGINT UNSIGNED | 是 | NULL | Estimate |
| `estimated_output_tokens` | BIGINT UNSIGNED | 是 | NULL | Estimate |
| `estimated_total_tokens` | BIGINT UNSIGNED | 是 | NULL | Estimate |
| `started_at` | DATETIME(3) | 是 | NULL | UTC |
| `completed_at` | DATETIME(3) | 是 | NULL | UTC |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | UTC |
| `updated_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3), ON UPDATE | UTC |

### 11.2 约束、索引与 FK

```text
PRIMARY KEY (id)
UNIQUE KEY uk_ai_analysis_batch_item_snapshot (batch_id, snapshot_id)
UNIQUE KEY uk_ai_analysis_batch_item_order (batch_id, selection_order)
KEY idx_ai_analysis_batch_item_status (batch_id, status)
KEY idx_ai_analysis_batch_item_analysis (analysis_id)
KEY idx_ai_analysis_batch_item_snapshot (snapshot_id)

batch_id       → ai_analysis_batch.id      RESTRICT
information_id → information_item.id       RESTRICT
snapshot_id    → information_snapshot.id   RESTRICT
analysis_id    → information_analysis.id   RESTRICT
```

所有 FK 都是 `ON DELETE RESTRICT ON UPDATE RESTRICT`。

Service 事务必须验证 `snapshot.information_id = information_id`。Item-limit 之外只记 Batch 计数；Token-budget 延后项保存为 Item。

## 12. `ai_model_invocation`

### 12.1 字段

| 字段 | 类型 | NULL | 默认 | 说明 |
|---|---|---:|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| `analysis_id` | BIGINT UNSIGNED | 否 | - | 所属 Analysis |
| `user_id` | BIGINT UNSIGNED | 否 | - | Owner/Usage 聚合 |
| `batch_item_id` | BIGINT UNSIGNED | 是 | NULL | 产生调用的 Batch Item |
| `provider` | VARCHAR(64) | 否 | - | Provider 标识 |
| `model_name` | VARCHAR(255) | 否 | - | 实际模型 |
| `provider_request_id` | VARCHAR(255) | 是 | NULL | Provider Request ID |
| `attempt_no` | INT UNSIGNED | 否 | - | Analysis 内从 1 递增 |
| `status` | VARCHAR(32) | 否 | `RUNNING` | RUNNING/SUCCEEDED/FAILED/TIMEOUT/UNKNOWN |
| `finish_reason` | VARCHAR(128) | 是 | NULL | Provider 完成原因 |
| `input_tokens` | BIGINT UNSIGNED | 是 | NULL | Provider Usage |
| `output_tokens` | BIGINT UNSIGNED | 是 | NULL | Provider Usage |
| `total_tokens` | BIGINT UNSIGNED | 是 | NULL | Provider Usage |
| `cached_input_tokens` | BIGINT UNSIGNED | 是 | NULL | Provider 可选 Usage |
| `reasoning_tokens` | BIGINT UNSIGNED | 是 | NULL | Provider 可选 Usage |
| `usage_status` | VARCHAR(32) | 否 | `UNAVAILABLE` | REPORTED/UNAVAILABLE |
| `latency_ms` | BIGINT UNSIGNED | 是 | NULL | 客户端观测毫秒 |
| `error_code` | VARCHAR(128) | 是 | NULL | 稳定错误码 |
| `error_message` | VARCHAR(2000) | 是 | NULL | 脱敏错误摘要 |
| `started_at` | DATETIME(3) | 否 | - | UTC |
| `completed_at` | DATETIME(3) | 是 | NULL | UTC |
| `created_at` | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | UTC |

### 12.2 约束、索引与 FK

```text
PRIMARY KEY (id)
UNIQUE KEY uk_ai_model_invocation_attempt (analysis_id, attempt_no)
KEY idx_ai_model_invocation_user_created (user_id, created_at)
KEY idx_ai_model_invocation_analysis_created (analysis_id, created_at)
KEY idx_ai_model_invocation_batch_item_created (batch_item_id, created_at)
KEY idx_ai_model_invocation_provider_request (provider, provider_request_id)

analysis_id   → information_analysis.id       RESTRICT
user_id       → user_account.id               RESTRICT
batch_item_id → ai_analysis_batch_item.id     RESTRICT
```

所有 FK 都是 `ON DELETE RESTRICT ON UPDATE RESTRICT`。

Provider 未报告 Usage：

```text
usage_status = UNAVAILABLE
input/output/total/cached/reasoning tokens = NULL
```

不得把 Estimate 填入以上字段。已报告 Usage 即使后续 Schema Validation 失败也必须保留。

## 13. Actual Usage 查询

```text
User:
  SUM(invocation.*_tokens) WHERE invocation.user_id = ?

Analysis:
  SUM(invocation.*_tokens) WHERE invocation.analysis_id = ?

Batch:
  invocation.batch_item_id
  → ai_analysis_batch_item.batch_id
  → SUM(invocation.*_tokens)
```

`NULL` 表示未知而不是 0。第一版不创建汇总表；性能问题出现后再以缓存方式扩展，Invocation 仍是事实源。

## 14. Scheduler 与 Worker 查询

Scheduler：

```sql
WHERE enabled = 1
  AND next_run_at <= :now
ORDER BY next_run_at, id
FOR UPDATE SKIP LOCKED
```

索引：`(enabled, next_run_at, id)`。

Worker 以 Batch Item 状态和稳定顺序领取；领取、完成分别使用短事务，Provider HTTP 调用不持有事务。第一版单并发，但行锁语义支持安全恢复。

## 15. 建表与 FK 顺序

TASK-024 必须先读取真实最新 Flyway 版本号，再创建下一条 migration；不得假定文件名。

单个 migration 内推荐顺序：

```text
1. ALTER information_item ADD FIRST_INGESTED index
2. CREATE user_account
3. CREATE ai_prompt_profile（暂不加 active_version_id FK）
4. CREATE ai_prompt_version
5. ALTER ai_prompt_profile ADD active_version_id FK
6. CREATE information_analysis
7. CREATE ai_analysis_schedule
8. CREATE ai_analysis_batch
9. CREATE ai_analysis_batch_item
10. CREATE ai_model_invocation
```

该顺序允许 `ai_model_invocation.batch_item_id` 直接建立 FK，不需要临时移除约束。

## 16. TASK-024 实施纪律

TASK-024 只能实现：

```text
本 Accepted 设计
+ 当前 DATABASE_DESIGN.md
+ 当前真实 Flyway
```

不得临场修改：

- 表数量；
- 字段语义和 NULL/default；
- PK/UNIQUE/核心 INDEX；
- FK/ON DELETE；
- Analysis 幂等；
- Actual Token 事实源；
- Batch/Schedule 幂等。

如发现真实冲突，必须停止 SQL 实施、修订设计并重新获得用户确认。

TASK-024 完成后：

1. 将真实 migration 文件名和最终 SQL 合并到 `docs/DATABASE_DESIGN.md`；
2. 校验空库全量 migrate；
3. 校验已有库 upgrade；
4. 对照 PO/Mapper/测试；
5. 若 SQL 与文档不一致，先修正并重新确认，不能静默漂移。
