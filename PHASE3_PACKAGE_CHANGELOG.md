# Phase 3 完整规划包 V2

版本日期：2026-08-02

## V2 相比上一版的数据库设计修订

本版新增：

- `docs/DATABASE_DESIGN_PHASE3_DRAFT.md`

并同步修改：

- `docs/PHASE3_DATA_MODEL_DRAFT.md`
- `docs/README.md`
- `docs/tasks/TASK-020.md`
- `docs/tasks/TASK-024.md`
- `docs/CODEX_PHASE3_WORKFLOW.md`
- `PHASE3_PACKAGE_CHANGELOG.md`
- `APPLY_INSTRUCTIONS.txt`

目标是把“Phase 3 领域模型”和“Phase 3 候选物理表设计”明确分离，并禁止 Codex 到 TASK-024 时临场自由设计数据库。

## 核心规划

- `docs/PHASE3_SCOPE.md`
- `docs/PHASE3_ARCHITECTURE_DRAFT.md`
- `docs/PHASE3_DATA_MODEL_DRAFT.md`
- `docs/DATABASE_DESIGN_PHASE3_DRAFT.md`
- `docs/CODEX_PHASE3_WORKFLOW.md`

## Phase 3 Database Design Draft

详细定义 8 张候选表：

- `user_account`
- `ai_prompt_profile`
- `ai_prompt_version`
- `information_analysis`
- `ai_model_invocation`
- `ai_analysis_batch`
- `ai_analysis_batch_item`
- `ai_analysis_schedule`

覆盖：

- 字段
- MySQL 类型建议
- NULL
- 默认值
- PK
- UNIQUE
- INDEX
- FK
- ON DELETE
- 状态枚举
- UTC / IANA timezone
- Analysis 幂等
- Prompt Version 不可变
- Model Invocation Actual Token
- Batch Candidate 冻结
- Schedule trigger 幂等
- Migration 顺序
- Session / Preview 是否需要额外表
- TASK-020 数据库专项审查
- TASK-024 实施纪律

## ADR

- `ADR-011-phase3-generic-ai-processing-boundary.md`
- `ADR-012-identity-prompt-version-and-analysis-ownership.md`
- `ADR-013-analysis-batch-budget-and-scheduling.md`

## Contracts

- `identity-mvp-v1.md`
- `ai-prompt-profile-v1.md`
- `information-analysis-v1.md`
- `analysis-batch-v1.md`
- `analysis-schedule-v1.md`
- `job-user-relevance-v1.md`

## TASK

- TASK-020 ～ TASK-032，共 13 个任务。

## 更新的项目文档

- `README.md`
- `docs/README.md`
- `docs/ROADMAP.md`
- `docs/CURRENT_STATUS.md`

## DATABASE_DESIGN.md 的处理

本包仍然不覆盖当前 `docs/DATABASE_DESIGN.md`。

原因：

- 它代表当前已落地数据库事实；
- Phase 3 表目前仍是 Proposed；
- TASK-020 只负责审查/冻结；
- TASK-024 真正实现 Flyway 并验收后，再把真实 Phase 3 结构合并进 `docs/DATABASE_DESIGN.md`。

本包也不直接覆盖现有：
- `AGENTS.md`
- `PROJECT_CONTEXT.md`
- `ARCHITECTURE.md`

TASK-020 在真实仓库审查后，只同步用户已经确认的长期 Accepted 原则。
