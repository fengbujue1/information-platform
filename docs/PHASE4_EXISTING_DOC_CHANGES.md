# Phase 4 Existing Document Change Plan

状态：Accepted

实施状态：Completed；Phase 4 启动与 TASK-044 收尾同步均已执行

## 1. README.md

Phase 4 启动：

- Phase 3 标为完成；
- Phase 4 标为进行中；
- 增加 Phase 4 文档入口；
- 不提前宣称未实现 Recommendation 已完成。

Phase 4 收尾再更新已实现能力。

## 2. docs/README.md

加入 Phase 4：

- Scope
- Architecture
- Data Model
- Database Design
- Logging Conventions
- Codex Workflow
- ADR-014 ～ ADR-018
- Recommendation Contracts
- TASK-033 ～ TASK-044

## 3. ROADMAP.md

Phase 4 改成 Accepted：

- Recommendation Profile；
- precomputed Run/Item；
- scoring/ranking；
- dedup/diversity；
- Analysis Batch 自动刷新；
- Manual Refresh；
- Feedback；
- BOSS contact disposition；
- Phase 4 Web。

明确不做：

- independent recommendation schedule；
- AI call；
- embedding/vector；
- BOSS chat auto sync；
- UI polish；
- production deployment。

## 4. CURRENT_STATUS.md

Phase 4 开始：

```text
Phase 4：进行中
当前任务：TASK-033
```

每 TASK 更新。

TASK-044 后：

```text
Phase 4：已完成
```

## 5. ARCHITECTURE.md

按实际实施进度增加：

```text
analysis
→ terminal event
→ recommendation
→ run/item
→ feed
→ interaction
```

数据库/代码未实施前不要写成“已实现”。

## 6. DATABASE_DESIGN.md

TASK-034 Flyway 完成后同步四张真实表、索引和 FK。
TASK-034A V4 完成后继续同步为当前六张表：四张 Generic Core/Run/Item 表与两张 JOB Extension 表。

草案不能冒充真实数据库事实。

## 7. backend/information-hub/AGENTS.md

增加：

```markdown
## Backend Operational Logging

详细规范：
`../../docs/LOGGING_CONVENTIONS.md`

所有新增/修改后端业务代码必须遵守。
```

并补充：

- recommendation 不调用 Provider；
- 无 independent Recommendation Cron；
- Feed 不实时重算；
- hard exclusion = NOT_INTERESTED / CONTACTED_NOT_SUITABLE；
- CONTACTED 不排除；
- 不自动读取 BOSS 聊天记录。

## 8. frontend AGENTS

TASK-043 如需追加：

- Profile；
- Refresh polling；
- Feed；
- Feedback；
- CONTACTED；
- CONTACTED_NOT_SUITABLE；
- stale 提示；
- Phase 4 不进行全站 UI redesign。
