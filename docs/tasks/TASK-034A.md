# TASK-034A：Phase 4 Recommendation Model Generalization

状态：DONE
所属阶段：Phase 4
优先级：P0
性质：TASK-034 后的数据模型通用化纠偏
前置任务：TASK-034 DONE
后续任务：完成后继续 TASK-035
设计决策：**已由用户确认，按本文实施，不需要再次讨论是否要通用化**

---

## 1. 背景

TASK-034 已创建：

```text
user_recommendation_profile
user_information_interaction
recommendation_run
recommendation_item
```

当前实现能够支持 Phase 4 的 JOB Recommendation，但其中两处把通用 Recommendation Core 与 JOB 领域模型混在了一起：

### 1.1 `user_recommendation_profile`

当前通用表直接包含：

```text
target_roles
preferred_skills
preferred_cities
preferred_remote_types
salary_min_monthly_yuan
excluded_keywords
```

并使用：

```text
UNIQUE(user_id)
```

这意味着一个用户只能有一个 Recommendation Profile，并且 Profile 字段直接绑定职位推荐。

未来平台接入：

```text
EDUCATION
MEDICAL
REAL_ESTATE
POLICY
NEWS
...
```

时会导致通用 Profile 不断增加领域专属 nullable 字段。

### 1.2 `user_information_interaction`

当前通用表包含：

```text
job_disposition
disposition_updated_at
```

`job_disposition` 是明确的 JOB Workflow 状态，不应存在于通用 Information Interaction Core 中。

---

## 2. 本任务目标

将 Phase 4 Recommendation 模型调整为：

> **通用 Recommendation Core + Information Type / Domain Extension**

设计理念与现有：

```text
information_item
    ↓
job_information
```

保持一致。

Phase 4 仍然只实际实现：

```text
informationType = JOB
```

本任务只保证未来增加其它 Information Type 时存在清晰扩展路径，不实现 EDUCATION / MEDICAL / REAL_ESTATE / POLICY / NEWS 业务。

---

## 3. 目标模型

### 3.1 Recommendation Profile Core

调整：

```text
user_recommendation_profile
```

只保存通用字段：

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

其中：

```text
informationType
```

使用与 `information_item.information_type` 一致的语义。

唯一约束从：

```text
UNIQUE(user_id)
```

调整为：

```text
UNIQUE(user_id, information_type)
```

因此未来允许：

```text
User 1
├── JOB Profile
├── EDUCATION Profile
├── REAL_ESTATE Profile
├── POLICY Profile
└── ...
```

Phase 4 当前只创建和使用 `JOB` Profile。

### 3.2 JOB Recommendation Profile Extension

新增：

```text
job_recommendation_profile
```

作为：

```text
user_recommendation_profile
        ↓ 1:1
job_recommendation_profile
```

的 JOB 专属扩展。

建议字段：

```text
profile_id
target_roles
preferred_skills
preferred_cities
preferred_remote_types
salary_min_monthly_yuan
excluded_keywords
created_at
updated_at
```

要求：

```text
profile_id
```

同时作为 Primary Key 和 FK → `user_recommendation_profile.id`，继续使用 `ON DELETE/UPDATE RESTRICT`。

JOB Profile Extension 不重复保存：

```text
user_id
information_type
analysis_prompt_profile_id
window_days
top_n
content_hash
```

Service 必须保证 `job_recommendation_profile` 只能关联 `information_type = JOB` 的 Core Profile。

---

## 4. Profile contentHash / Snapshot 语义

`user_recommendation_profile.content_hash` 继续保留，但其语义调整为：

> 当前 Information Type 的**完整组合 Recommendation Profile** 的 canonical hash。

对于 JOB：

```text
Profile Core
+
Job Recommendation Profile Extension
        ↓
canonical representation
        ↓
SHA-256
```

因此修改 JOB Extension 中任一推荐字段时，`content_hash` 必须重新计算。

`recommendation_run.profile_snapshot_json` 必须保存当次运行使用的**完整组合 Profile Snapshot**，至少包括：

```text
informationType
analysisPromptProfileId
windowDays
topN
JOB domain preferences
```

---

## 5. User Information Interaction Core

调整：

```text
user_information_interaction
```

保留通用字段：

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

移除 JOB 专属：

```text
jobDisposition
dispositionUpdatedAt
```

通用 Feedback 保持：

```text
NONE
INTERESTED
NOT_INTERESTED
```

通用 hard exclusion：

```text
feedbackState = NOT_INTERESTED
```

---

## 6. JOB Disposition Extension

新增：

```text
user_job_disposition
```

推荐采用 Interaction Extension 方式：

```text
user_information_interaction
        ↓ 1:1
user_job_disposition
```

建议字段：

```text
interaction_id
job_disposition
disposition_updated_at
created_at
updated_at
```

其中 `interaction_id` 同时作为 PK 和 FK → `user_information_interaction.id`，使用 `ON DELETE/UPDATE RESTRICT`。

状态保持：

```text
NONE
CONTACTED
CONTACTED_NOT_SUITABLE
```

语义保持：

```text
CONTACTED
→ 不属于 hard exclusion

CONTACTED_NOT_SUITABLE
→ JOB Recommendation hard exclusion
```

Service 必须校验对应 `information_item.information_type = JOB`。

---

## 7. Recommendation Run 通用化

`recommendation_run` 增加：

```text
information_type
```

并在 Run 创建时冻结。

Run 必须明确属于哪个 Information Type，不能只依赖 `algorithm_key` 或 `profile_id` 间接推断。

重新评估现有索引：

```text
(user_id, status, created_at)
(user_id, completed_at, id)
```

是否需要调整/补充为包含 `information_type` 的组合索引。

必须通过真实 SQL + `EXPLAIN` 决定，避免机械重复添加冗余索引。

---

## 8. Recommendation Item

原则上不修改 `recommendation_item` 核心结构。

继续关联：

```text
information_item
information_snapshot
information_analysis
recommendation_run
```

继续保留：

```text
final_score
ai_relevance_score
profile_match_score
freshness_score
score_breakdown_json
reasons_json
duplicate_group_key
```

未来领域特有的分数组成可进入 `score_breakdown_json`。

不要求未来所有领域复用 JOB 的 70/20/10 权重。

---

## 9. Algorithm Boundary

Phase 4 当前继续：

```text
algorithmKey = JOB_RECOMMENDATION
algorithmVersion = 1
```

以及：

```text
70% AI relevance
20% JOB profile match
10% freshness
```

这属于 `JOB_RECOMMENDATION / V1`，不是整个 Information Platform 的永久统一算法。

未来可新增：

```text
EDUCATION_RECOMMENDATION
REAL_ESTATE_RECOMMENDATION
POLICY_RECOMMENDATION
MEDICAL_RECOMMENDATION
```

各自独立版本化。

本任务不要修改 JOB_RECOMMENDATION V1 的已确认算法规则。

---

## 10. Candidate Resolver Boundary

TASK-037 后续仍然只实现 JOB Candidate Resolver。

架构上保持：

```text
Recommendation Core
        ↓
Information-Type-specific Candidate / Profile / Algorithm
```

本任务不实施 Candidate Resolver，只保证模型和文档留出领域扩展路径。

---

## 11. API / Contract 调整

TASK-035 尚未实施，因此现在必须在 API 落地前修正 Contract。

### 11.1 Recommendation Profile

Recommendation Profile API 应显式表达 `informationType`。

建议调整为：

```http
GET /api/v1/recommendation/profiles/{informationType}
PUT /api/v1/recommendation/profiles/{informationType}
```

Phase 4 当前只实现：

```text
informationType = JOB
```

对外 DTO 可以继续返回组合后的 JOB Profile：

```text
Core Fields
+
JOB Extension Fields
```

不要求前端感知数据库内部拆表。

### 11.2 Manual Refresh / Feed / Run Query

尚未实现的 Recommendation API 也要能够明确 `informationType`。

必须达到：

```text
Manual Refresh JOB
→ 只刷新 JOB Recommendation

GET JOB Feed
→ 只读取 JOB 最新 COMPLETED Run
```

具体使用 query/path/body 哪一种，请遵循当前项目 API 风格并更新 Contract。

---

## 12. Flyway 迁移要求

当前已经存在并执行过：

```text
V3__create_phase4_recommendation_tables.sql
```

**禁止修改 V3。**

开始任务前检查真实：

```text
backend/information-hub/src/main/resources/db/migration/
```

如果 V3 仍是最新版本，则新增下一 Migration，例如：

```text
V4__generalize_phase4_recommendation_model.sql
```

如果已有更新 Migration，则使用真实下一个版本号。

### 12.1 必须保留已有 V3 数据

即使当前开发库可能为空，也不得假设为空。

Profile：

1. Core Profile 回填 `information_type = JOB`；
2. 将 JOB 专属字段复制到 `job_recommendation_profile`；
3. 验证后从 Core 删除 JOB 专属列；
4. UNIQUE 改为 `(user_id, information_type)`。

Interaction：

1. 将 `job_disposition` / `disposition_updated_at` 复制到 `user_job_disposition`；
2. 校验对应 Information 为 JOB；
3. 验证后从通用 Interaction 删除 JOB 专属列。

Run：

1. 为已有 Run 回填 `information_type`；
2. 再调整为 NOT NULL；
3. 更新必要索引。

---

## 13. 应用代码修改

本任务只修改 Data Model 层所需代码。

预计包括：

- `UserRecommendationProfilePO`
  - 新增 `informationType`
  - 删除 JOB 专属字段；
- 新增 `JobRecommendationProfilePO`；
- 新增对应 Mapper；
- `UserInformationInteractionPO`
  - 删除 `jobDisposition`
  - 删除 `dispositionUpdatedAt`；
- 新增 `UserJobDispositionPO`；
- 新增对应 Mapper；
- `RecommendationRunPO`
  - 新增 `informationType`；
- `FeedbackState`
  - 保持通用；
- `JobDisposition`
  - 保持 JOB-specific 语义，如当前包位置过于通用，应移动到合适的 JOB/domain 位置。

具体 package 遵循当前项目按业务模块组织的既有风格，不做无关的大规模重构。

---

## 14. 文档同步

至少同步：

```text
docs/PHASE4_SCOPE.md
docs/PHASE4_ARCHITECTURE_DRAFT.md
docs/PHASE4_DATA_MODEL_DRAFT.md
docs/DATABASE_DESIGN_PHASE4_DRAFT.md
docs/DATABASE_DESIGN.md
docs/CURRENT_STATUS.md
```

检查并更新受影响 Contract：

```text
recommendation-profile-v1.md
recommendation-refresh-v1.md
recommendation-feed-v1.md
recommendation-interaction-v1.md
```

新增 ADR，建议：

```text
ADR-018-phase4-recommendation-domain-generalization.md
```

ADR 至少冻结：

- Generic Core + Information-Type-specific Extension；
- Profile Core 按 `(user, informationType)` 唯一；
- JOB Preference 使用独立扩展模型；
- 通用 Interaction 与 JOB Disposition 分离；
- Recommendation Run 显式冻结 `informationType`；
- Domain-specific Algorithm 独立版本化；
- Phase 4 实际仍只实现 JOB。

---

## 15. TASK 状态

新增：

```text
docs/tasks/TASK-034A.md
```

开始：

```text
TASK-034 = DONE
TASK-034A = IN_PROGRESS
TASK-035 = TODO
```

完成：

```text
TASK-034 = DONE
TASK-034A = DONE
TASK-035 = TODO / next
```

同步 `docs/CURRENT_STATUS.md`。

不要提前实施 TASK-035。

---

## 16. 测试要求

至少覆盖：

### Migration

- V3 → 新 Migration 成功；
- 空库 V1 → latest 成功（环境允许时）；
- 已有 Profile 数据正确迁移到 JOB Extension；
- 已有 Interaction disposition 正确迁移；
- 已有 Run 正确获得 `information_type`；
- 新 FK / UNIQUE / INDEX 正确；
- V3 未修改。

### Mapper / Persistence

- 同一用户可以有不同 `information_type` Profile；
- 同一用户同一 `information_type` 唯一；
- JOB Extension 与 Core 1:1；
- Interaction Core CRUD；
- Job Disposition Extension CRUD；
- Recommendation Run `informationType` 映射；
- `FeedbackState` 保持通用；
- `CONTACTED` 仍不排除；
- `CONTACTED_NOT_SUITABLE` 仍属于 JOB hard exclusion。

### Regression

同步更新 TASK-034 已有测试并继续通过。

按当前 backend AGENTS 执行完整相关测试；条件允许时执行完整：

```text
.\mvnw.cmd test
```

---

## 17. 明确不做

本任务不要实施：

- TASK-035 Recommendation Profile API 业务代码；
- TASK-036 Interaction API；
- TASK-037 Candidate Resolver；
- Scoring；
- Ranking；
- Worker；
- Feed；
- Web；
- EDUCATION / MEDICAL / REAL_ESTATE / POLICY 等真实领域；
- Generic JSON 万能 Profile；
- EAV Profile；
- Vector DB；
- RAG；
- Redis / Kafka；
- 大规模 package 重构。

本任务唯一目标：

> **在 TASK-035 之前把 Recommendation Core 的领域边界纠正好。**

---

## 18. Git 规则

开始前：

```text
git status --short
git log --oneline -8
git diff --check
```

若当前真实代码与本任务设计一致，直接实施，不需要再次等待用户确认。

只有发现必须偏离本文设计、扩大 Scope、修改其它 Accepted 决策或执行破坏性操作时才停止询问。

完成后：

1. 运行测试；
2. `git diff --check`；
3. 检查没有进入 TASK-035；
4. 更新 TASK / CURRENT_STATUS / 事实文档；
5. 只 `git add` 本任务文件；
6. 不暂存任务开始前已有无关修改；
7. `git diff --cached --check`；
8. `git status --short`；
9. 不 commit；
10. 不 push。

---

## 19. 最终汇报

必须汇报：

- 实际 Migration 编号；
- Schema 调整；
- V3 数据迁移策略；
- 新增/修改 PO / Mapper；
- Contract 调整；
- ADR / 文档调整；
- 测试命令与结果；
- V3 是否保持未修改；
- staged 文件；
- 最终 `git status --short`；
- TASK-035 是否仍未实施；
- 建议 commit message。

建议：

```text
refactor(recommendation): generalize Phase 4 recommendation domain model
```

---

## 20. 实施记录（2026-08-09）

- 新增 `V4__generalize_phase4_recommendation_model.sql`，V3 保持原始 SHA-256 `cf5ff78fe0421879f6151e3034b3b71ebe9db3b7d413b8a33c02205fc1773aae`；
- V3 Profile 全部回填为 JOB，偏好复制到 `job_recommendation_profile`，核对数量后从 Core 删除 JOB 列；
- 迁移前拒绝非 JOB Information 上的有效 JOB disposition；JOB Interaction 状态复制到 `user_job_disposition`，核对数量后从 Core 删除 JOB 列；
- V3 Run 从 Profile 回填 `information_type`，历史 `profile_snapshot_json` 同步补充 `informationType`；
- Profile 唯一约束改为 `(user_id, information_type)`，Run 两个 Owner 查询索引加入 Information Type；
- PO/Mapper 调整为 Generic Core + `recommendation.job` Extension；`JobDisposition` 移入 JOB domain；
- 新增 ADR-018，并同步 Scope、Architecture、Data Model、Database Design 与四份 Recommendation Contract；
- Phase 4 仍只实际支持 JOB，TASK-035 未实施。

验证结果：

- Java 21.0.11 / Maven Wrapper 3.9.16；
- 专用 MySQL 测试库真实 V3 → V4 成功，既有 Profile、Interaction、Run 数据逐字段验证后清理测试数据；
- V3 checksum、状态语义、V4 Schema/FK/UNIQUE/INDEX、EXPLAIN、Core/Extension Mapper CRUD 与 Phase 3 migration 回归通过；
- 完整后端回归 `./mvnw.cmd test`：191 tests，0 failures，0 errors，2 skipped；
- 空库 V1 → V4 测试已更新；是否执行取决于专用 empty 测试库环境变量；
- 未修改 V1/V2/V3，未实施 TASK-035 或后续业务。
