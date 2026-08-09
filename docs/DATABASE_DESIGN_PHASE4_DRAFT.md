# Database Design Phase 4

状态：Accepted

实施状态：TASK-034 已通过 V3 实施

数据库：MySQL 8.x

Migration：Flyway

> 实施时必须先检查真实 `db/migration`，再确定实际下一个版本号；不得修改已执行 migration。

## 1. 新增表

```text
user_recommendation_profile
user_information_interaction
recommendation_run
recommendation_item
```

## 2. user_recommendation_profile

| 字段 | 类型 | NULL | 说明 |
|---|---|---:|---|
| id | BIGINT UNSIGNED | 否 | PK |
| user_id | BIGINT UNSIGNED | 否 | Owner |
| analysis_prompt_profile_id | BIGINT UNSIGNED | 否 | 绑定 AI Prompt Profile |
| window_days | INT UNSIGNED | 否 | 默认 7，1..30 |
| top_n | INT UNSIGNED | 否 | 默认 50，1..100 |
| target_roles | JSON | 是 | 字符串数组 |
| preferred_skills | JSON | 是 | 字符串数组 |
| preferred_cities | JSON | 是 | 字符串数组 |
| preferred_remote_types | JSON | 是 | 字符串数组 |
| salary_min_monthly_yuan | INT UNSIGNED | 是 | 最低期望月薪 |
| excluded_keywords | JSON | 是 | hard exclusion 关键词 |
| content_hash | CHAR(64) | 否 | SHA-256 |
| created_at | DATETIME(3) | 否 | UTC |
| updated_at | DATETIME(3) | 否 | UTC |

约束：

```text
UNIQUE (user_id)
INDEX (analysis_prompt_profile_id)
FK user_id -> user_account.id RESTRICT
FK analysis_prompt_profile_id -> ai_prompt_profile.id RESTRICT
```

Service 校验 Prompt Profile Owner 与 userId 相同。

## 3. user_information_interaction

| 字段 | 类型 | NULL | 说明 |
|---|---|---:|---|
| id | BIGINT UNSIGNED | 否 | PK |
| user_id | BIGINT UNSIGNED | 否 | Owner |
| information_id | BIGINT UNSIGNED | 否 | Information |
| view_count | INT UNSIGNED | 否 | 默认 0 |
| last_viewed_at | DATETIME(3) | 是 | 最近查看 |
| feedback_state | VARCHAR(32) | 否 | NONE/INTERESTED/NOT_INTERESTED |
| feedback_updated_at | DATETIME(3) | 是 | Feedback 修改时间 |
| job_disposition | VARCHAR(32) | 否 | NONE/CONTACTED/CONTACTED_NOT_SUITABLE |
| disposition_updated_at | DATETIME(3) | 是 | 求职处理状态修改时间 |
| last_recommendation_item_id | BIGINT UNSIGNED | 是 | 最近一次 Recommendation 归因 |
| created_at | DATETIME(3) | 否 | UTC |
| updated_at | DATETIME(3) | 否 | UTC |

约束：

```text
UNIQUE (user_id, information_id)
INDEX (user_id, feedback_state, job_disposition, updated_at)
INDEX (information_id)
FK user_id -> user_account.id RESTRICT
FK information_id -> information_item.id RESTRICT
```

`last_recommendation_item_id`：

实施时可采用：

- nullable FK，在 recommendation_item 创建后 ALTER；
- 或只保留应用校验。

TASK-034 必须选择并记录，避免循环建表顺序问题。

实施选择：采用 nullable FK。V3 先创建 `user_information_interaction`，在 `recommendation_item` 创建后通过 `ALTER TABLE` 补充 `ON DELETE/UPDATE RESTRICT` FK。

数据库不使用 ENUM，合法值由 Java 校验，遵守现有数据库原则。

## 4. recommendation_run

| 字段 | 类型 | NULL | 说明 |
|---|---|---:|---|
| id | BIGINT UNSIGNED | 否 | PK |
| user_id | BIGINT UNSIGNED | 否 | Owner |
| trigger_type | VARCHAR(32) | 否 | MANUAL / ANALYSIS_BATCH_COMPLETED |
| source_analysis_batch_id | BIGINT UNSIGNED | 是 | Auto 来源 |
| profile_id | BIGINT UNSIGNED | 否 | Profile |
| profile_content_hash | CHAR(64) | 否 | Frozen hash |
| profile_snapshot_json | JSON | 否 | Frozen Profile |
| prompt_profile_id | BIGINT UNSIGNED | 否 | Frozen Prompt Profile |
| prompt_version_id | BIGINT UNSIGNED | 否 | Frozen Prompt Version |
| algorithm_key | VARCHAR(64) | 否 | JOB_RECOMMENDATION |
| algorithm_version | INT UNSIGNED | 否 | V1=1 |
| window_start | DATETIME(3) | 否 | inclusive |
| window_end | DATETIME(3) | 否 | exclusive |
| candidate_count | INT UNSIGNED | 否 | 默认 0 |
| eligible_count | INT UNSIGNED | 否 | 默认 0 |
| result_count | INT UNSIGNED | 否 | 默认 0 |
| status | VARCHAR(32) | 否 | lifecycle |
| skip_reason | VARCHAR(64) | 是 | NOOP |
| failure_code | VARCHAR(64) | 是 | stable code |
| failure_message | VARCHAR(1000) | 是 | diagnostic |
| started_at | DATETIME(3) | 是 | UTC |
| completed_at | DATETIME(3) | 是 | UTC |
| created_at | DATETIME(3) | 否 | UTC |
| updated_at | DATETIME(3) | 否 | UTC |

索引：

```text
INDEX (user_id, status, created_at)
INDEX (user_id, completed_at, id)
UNIQUE (source_analysis_batch_id)
```

FK：

```text
user_account
ai_analysis_batch
user_recommendation_profile
ai_prompt_profile
ai_prompt_version
```

`source_analysis_batch_id` UNIQUE 允许多个 NULL，使 Manual Run 不冲突。

## 5. recommendation_item

| 字段 | 类型 | NULL | 说明 |
|---|---|---:|---|
| id | BIGINT UNSIGNED | 否 | PK |
| run_id | BIGINT UNSIGNED | 否 | Run |
| information_id | BIGINT UNSIGNED | 否 | Information |
| snapshot_id | BIGINT UNSIGNED | 否 | Frozen Snapshot |
| analysis_id | BIGINT UNSIGNED | 否 | Used Analysis |
| rank_no | INT UNSIGNED | 否 | 1-based |
| final_score | DECIMAL(6,3) | 否 | 0..100 |
| ai_relevance_score | DECIMAL(6,3) | 否 | 0..100 |
| profile_match_score | DECIMAL(6,3) | 否 | 0..100 |
| freshness_score | DECIMAL(6,3) | 否 | 0..100 |
| score_breakdown_json | JSON | 否 | Explainability |
| reasons_json | JSON | 否 | 用户原因 |
| duplicate_group_key | CHAR(64) | 否 | deterministic |
| created_at | DATETIME(3) | 否 | UTC |

约束：

```text
UNIQUE (run_id, information_id)
UNIQUE (run_id, rank_no)
INDEX (information_id)
INDEX (analysis_id)
```

FK：

```text
recommendation_run
information_item
information_snapshot
information_analysis
```

## 6. Feed 与 Interaction

Feed 查询需要对当前用户 Interaction 应用：

```text
feedback_state != NOT_INTERESTED
AND
job_disposition != CONTACTED_NOT_SUITABLE
```

因此建议确保：

```text
(user_id, information_id)
```

唯一索引可高效关联。

不删除 `recommendation_item`。

## 7. Candidate 与 Interaction

Candidate Resolver 同样排除：

```text
NOT_INTERESTED
CONTACTED_NOT_SUITABLE
```

这使 hard exclusion 对未来 Run 持续生效。

`CONTACTED` 不排除。

## 8. Candidate 索引

优先复用现有：

- Information first_seen_time；
- current Snapshot；
- Analysis user/prompt/snapshot/status；
- Interaction unique index。

TASK-034/037 必须基于真实 SQL 执行 EXPLAIN，再决定是否新增组合索引。

TASK-034 已基于合法测试数据图验证 Profile Owner、Interaction identity、Run Owner/Status 与 Item Run/Rank 查询索引；Candidate Resolver 的最终 SQL 与补充索引仍由 TASK-037 决定。

## 9. 时间

所有数据库时间：

```text
UTC
DATETIME(3)
```

windowStart/windowEnd 在 Run 创建时冻结。

## 10. Migration

```text
check real latest migration
→ add next migration only
→ empty DB test
→ previous-version upgrade test
→ mapper integration test
```

Flyway SQL 是最终数据库事实来源。

实际 Migration：

```text
V3__create_phase4_recommendation_tables.sql
```
