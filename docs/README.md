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

## Phase 4

状态：**进行中。**

当前任务：`TASK-037`（尚未实施）。

Phase 4 规划基线已 Accepted，TASK-033～TASK-036（含 TASK-034A）已完成；当前持久化结构为 V4 Generic Core + JOB Extension，Recommendation Profile 与 Interaction Backend/API 已落地，后续从 TASK-037 起按顺序实施。

- [Phase 4 正式规划包](../PHASE4_PACKAGE_MANIFEST.md)
- [Phase 4 Scope](PHASE4_SCOPE.md)
- [Phase 4 Architecture（Accepted）](PHASE4_ARCHITECTURE_DRAFT.md)
- [Phase 4 Data Model（Accepted）](PHASE4_DATA_MODEL_DRAFT.md)
- [Phase 4 Database Design（Accepted，V4 已实施）](DATABASE_DESIGN_PHASE4_DRAFT.md)
- [Backend Operational Logging Conventions](LOGGING_CONVENTIONS.md)
- [Codex Phase 4 Workflow](CODEX_PHASE4_WORKFLOW.md)
- [Phase 4 Existing Document Change Plan](PHASE4_EXISTING_DOC_CHANGES.md)

### ADR

- [ADR-014：Precomputed Recommendation](decisions/ADR-014-phase4-precomputed-recommendation.md)
- [ADR-015：Recommendation Trigger Lifecycle](decisions/ADR-015-phase4-recommendation-trigger-lifecycle.md)
- [ADR-016：Profile、Feedback 与 Job Disposition Boundary](decisions/ADR-016-phase4-profile-feedback-job-disposition-boundary.md)
- [ADR-017：Backend Operational Logging Baseline](decisions/ADR-017-backend-operational-logging-baseline.md)
- [ADR-018：Recommendation Generic Core + Domain Extension](decisions/ADR-018-phase4-recommendation-domain-generalization.md)

### Contracts

- [Recommendation Profile V1](contracts/recommendation-profile-v1.md)
- [Recommendation Refresh V1](contracts/recommendation-refresh-v1.md)
- [Recommendation Feed V1](contracts/recommendation-feed-v1.md)
- [Recommendation Interaction V1](contracts/recommendation-interaction-v1.md)

### TASK

- [TASK-033：Backend Operational Logging Baseline](tasks/TASK-033.md)
- [TASK-034：Recommendation Data Model 与 Flyway](tasks/TASK-034.md)
- [TASK-034A：Recommendation Model Generalization](tasks/TASK-034A.md)
- [TASK-035：Recommendation Profile Backend 与 API](tasks/TASK-035.md)
- [TASK-036：User Interaction、Feedback 与 Job Disposition](tasks/TASK-036.md)
- [TASK-037：Recommendation Candidate Resolver](tasks/TASK-037.md)
- [TASK-038：Recommendation Scoring 与 Explainability](tasks/TASK-038.md)
- [TASK-039：Ranking、Deduplication、Diversity 与 Top N](tasks/TASK-039.md)
- [TASK-040：Recommendation Run、Worker 与 Manual Refresh](tasks/TASK-040.md)
- [TASK-041：Analysis Batch Completion → Recommendation Auto Trigger](tasks/TASK-041.md)
- [TASK-042：Recommendation Feed Query API](tasks/TASK-042.md)
- [TASK-043：Phase 4 Web](tasks/TASK-043.md)
- [TASK-044：Full-stack E2E、验收与收尾](tasks/TASK-044.md)
