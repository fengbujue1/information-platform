# Database Design Phase 4

状态：Accepted

实施状态：TASK-034 V3 + TASK-034A V4 已实施

数据库：MySQL 8.x；Migration：Flyway。

> V3 已执行且不可修改。通用化纠偏由 V4 完成，Flyway SQL 是最终物理事实来源。

## 1. 当前六张表

```text
user_recommendation_profile
job_recommendation_profile
user_information_interaction
user_job_disposition
recommendation_run
recommendation_item
```

## 2. user_recommendation_profile（Generic Core）

| 字段 | 类型 | NULL | 说明 |
|---|---|---:|---|
| id | BIGINT UNSIGNED | 否 | PK |
| user_id | BIGINT UNSIGNED | 否 | Owner |
| information_type | VARCHAR(32) | 否 | 与 Information Type 同语义；当前只用 JOB |
| analysis_prompt_profile_id | BIGINT UNSIGNED | 否 | 绑定 AI Prompt Profile |
| window_days | INT UNSIGNED | 否 | 默认 7，1..30 |
| top_n | INT UNSIGNED | 否 | 默认 50，1..100 |
| content_hash | CHAR(64) | 否 | Core + Domain Extension 完整 SHA-256 |
| created_at / updated_at | DATETIME(3) | 否 | UTC |

约束：

```text
UNIQUE (user_id, information_type)
INDEX (analysis_prompt_profile_id)
FK user_id -> user_account.id RESTRICT
FK analysis_prompt_profile_id -> ai_prompt_profile.id RESTRICT
```

同 Owner、Prompt 用途和领域类型由 Service 校验。

## 3. job_recommendation_profile（JOB Extension）

| 字段 | 类型 | NULL | 说明 |
|---|---|---:|---|
| profile_id | BIGINT UNSIGNED | 否 | PK + FK Core |
| target_roles | JSON | 是 | 目标岗位 |
| preferred_skills | JSON | 是 | 偏好技能 |
| preferred_cities | JSON | 是 | 偏好城市 |
| preferred_remote_types | JSON | 是 | 偏好远程类型 |
| salary_min_monthly_yuan | INT UNSIGNED | 是 | 最低月薪，元 |
| excluded_keywords | JSON | 是 | JOB hard exclusion 关键词 |
| created_at / updated_at | DATETIME(3) | 否 | UTC |

`profile_id -> user_recommendation_profile.id RESTRICT`。Service 必须保证 Core `information_type = JOB`。

## 4. user_information_interaction（Generic Core）

| 字段 | 类型 | NULL | 说明 |
|---|---|---:|---|
| id | BIGINT UNSIGNED | 否 | PK |
| user_id | BIGINT UNSIGNED | 否 | Owner |
| information_id | BIGINT UNSIGNED | 否 | Information |
| view_count | INT UNSIGNED | 否 | 默认 0 |
| last_viewed_at | DATETIME(3) | 是 | UTC |
| feedback_state | VARCHAR(32) | 否 | NONE/INTERESTED/NOT_INTERESTED |
| feedback_updated_at | DATETIME(3) | 是 | UTC |
| last_recommendation_item_id | BIGINT UNSIGNED | 是 | 最近归因 Item |
| created_at / updated_at | DATETIME(3) | 否 | UTC |

约束与索引：

```text
UNIQUE (user_id, information_id)
INDEX (user_id, feedback_state, updated_at)
INDEX (information_id)
FK user/item/information 全部 RESTRICT
```

数据库不使用 ENUM；Feedback 合法值由 Java 校验。

## 5. user_job_disposition（JOB Extension）

| 字段 | 类型 | NULL | 说明 |
|---|---|---:|---|
| interaction_id | BIGINT UNSIGNED | 否 | PK + FK Interaction Core |
| job_disposition | VARCHAR(32) | 否 | 默认 NONE |
| disposition_updated_at | DATETIME(3) | 是 | UTC |
| created_at / updated_at | DATETIME(3) | 否 | UTC |

`interaction_id -> user_information_interaction.id RESTRICT`。Service 必须校验对应 Information 为 JOB。

## 6. recommendation_run

V3 字段全部保留，并由 V4 新增：

```text
information_type VARCHAR(32) NOT NULL
```

Run 冻结 Owner、Information Type、Profile hash/snapshot、Prompt Profile/Version、Algorithm 和窗口。关键索引：

```text
UNIQUE (source_analysis_batch_id)
INDEX (user_id, information_type, status, created_at)
INDEX (user_id, information_type, completed_at, id)
```

所有 FK 继续 RESTRICT；多个 NULL source Batch 允许 Manual Run 共存。

## 7. recommendation_item

V4 不修改 Item：

```text
UNIQUE (run_id, information_id)
UNIQUE (run_id, rank_no)
INDEX (information_id)
INDEX (snapshot_id)
INDEX (analysis_id)
```

Item 继续绑定 Run、Information、不可变 Snapshot 和 Analysis，全部 RESTRICT。

## 8. Visibility 与 Hard Exclusion

通用：

```text
feedback_state = NOT_INTERESTED
```

JOB 额外：

```text
job_disposition = CONTACTED_NOT_SUITABLE
```

`CONTACTED` 不排除。Feed 与 Candidate 必须同时读取 Core Interaction 和 JOB extension，不删除历史 Item。

## 9. V4 数据迁移

实际 migration：

```text
V4__generalize_phase4_recommendation_model.sql
```

顺序：

1. 迁移前验证非 JOB Interaction 不含有效 JOB disposition；
2. V3 Profile 回填 `information_type = JOB`；
3. 复制 JOB 偏好到 `job_recommendation_profile` 并核对数量；
4. Profile 唯一约束改为 `(user_id, information_type)`，删除 Core JOB 列；
5. 仅为 JOB Interaction 复制 `user_job_disposition` 并核对数量；
6. 删除 Interaction Core 的 JOB 列并收窄通用状态索引；
7. Run 从 Profile 回填 `information_type`，并给历史 snapshot JSON 补充该字段；
8. Run `information_type` 收紧为 NOT NULL，索引增加 Information Type。

V3 `content_hash` 已覆盖完整 JOB Profile，拆表后保持原值和完整组合语义。

## 10. EXPLAIN 与后续索引

TASK-034A 已在合法数据图上验证：

```text
Profile: user_id + information_type
Interaction: user_id + information_id
Run: user_id + information_type + status
Item: run_id + rank_no
```

Candidate Resolver 最终 SQL 和额外索引仍由 TASK-037 通过真实 EXPLAIN 决定。

## 11. 当前不创建

- EDUCATION/MEDICAL/REAL_ESTATE/POLICY 等扩展表；
- Recommendation Schedule/Batch/Profile Version；
- Generic JSON 万能 Profile 或 EAV；
- Notification 表。
