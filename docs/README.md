# Information Platform 文档索引

## 核心设计

- [项目背景](PROJECT_CONTEXT.md)
- [总体架构](ARCHITECTURE.md)
- [数据库设计](DATABASE_DESIGN.md)
- [路线图](ROADMAP.md)
- [当前状态](CURRENT_STATUS.md)

## Phase 1

状态：已完成。

- InformationEnvelope V1
- BOSS Collector 集成
- Information Hub
- MySQL/Flyway
- 幂等归档与 Snapshot
- Outbox
- Job Query API
- 真实 E2E

## Phase 2

状态：已完成。

- [Phase 2 Scope](PHASE2_SCOPE.md)
- [Web UI Behavior V1](contracts/web-ui-behavior-v1.md)
- [ADR-010：Phase 2 Web MVP 边界](decisions/ADR-010-phase2-web-mvp-boundary.md)

## Phase 3

状态：**已完成。**

- [Phase 3 Scope](PHASE3_SCOPE.md)
- [Phase 3 Architecture（Accepted / Implemented）](PHASE3_ARCHITECTURE_DRAFT.md)
- [Phase 3 Data Model（Accepted / Implemented）](PHASE3_DATA_MODEL_DRAFT.md)
- [Phase 3 Database Design（Accepted / Implemented）](DATABASE_DESIGN_PHASE3_DRAFT.md)
- [Phase 3 完成总结](PHASE3_COMPLETION.md)

### ADR

- [ADR-011：通用 AI Processing 边界](decisions/ADR-011-phase3-generic-ai-processing-boundary.md)
- [ADR-012：Identity、Prompt Version 与 Analysis Ownership](decisions/ADR-012-identity-prompt-version-and-analysis-ownership.md)
- [ADR-013：Batch、Budget 与 Schedule](decisions/ADR-013-analysis-batch-budget-and-scheduling.md)

### Contracts

- [Identity MVP V1](contracts/identity-mvp-v1.md)
- [AI Prompt Profile V1](contracts/ai-prompt-profile-v1.md)
- [Information Analysis V1](contracts/information-analysis-v1.md)
- [Analysis Batch V1](contracts/analysis-batch-v1.md)
- [Analysis Schedule V1](contracts/analysis-schedule-v1.md)
- [JOB User Relevance V1](contracts/job-user-relevance-v1.md)
- [JOB User Relevance V2](contracts/job-user-relevance-v2.md)

### TASK

- TASK-020 ～ TASK-032：全部完成。
- 最终验收记录：[TASK-032](tasks/TASK-032.md)

## 当前阶段

Phase 3 已归档。

Phase 4 尚未启动。Phase 4 开始前需创建独立 Scope、ADR、Contract 和 TASK。
