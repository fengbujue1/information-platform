# Phase 4 Official Planning Package

状态：Accepted / Ready for Implementation

阶段：Phase 4 — Personalized Recommendation MVP

基线：Phase 3 已完成
规划日期：2026-08-07

## 1. 使用方式

本包是 Phase 4 正式规划基线。

从 Phase 4 实施开始：

- Scope、Architecture、Data Model、ADR、Contract 按本包执行；
- TASK-033 ～ TASK-044 按顺序实施；
- 一个 Codex 实施上下文只处理一个明确 TASK；
- 后端 TASK 必须遵守 `docs/LOGGING_CONVENTIONS.md`；
- 不得在当前 TASK 中提前实现后续阶段功能。

本包的设计已经冻结；字段或行为若在实施中必须发生不兼容变化，应先更新相应 ADR / Contract，再修改代码。

## 2. Phase 4 正式目标

```text
Phase 3 USER_RELEVANCE Analysis
        ↓
Recommendation Profile
        ↓
Candidate Resolver
        ↓
Deterministic Scoring
        ↓
Ranking / Dedup / Diversity
        ↓
Recommendation Run / Item
        ↓
Recommendation Feed
        ↓
User Interaction / Job Disposition
```

## 3. 已冻结关键决策

1. Recommendation 使用预计算结果，Feed 查询不实时跑推荐算法。
2. Recommendation 不新增 LLM 调用，只消费 Phase 3 已有成功 `JOB_USER_RELEVANCE` Analysis。
3. Recommendation 业务刷新入口只有：
   - Analysis Batch 完成后自动刷新；
   - 用户手动刷新。
4. 不增加独立 Recommendation Cron。
5. Recommendation Worker 仅消费已经存在的 PENDING Run，不构成第三种业务 Trigger。
6. Profile 修改不自动刷新。
7. Manual Refresh 不自动执行 AI Analysis。
8. Analysis Batch `COMPLETED` / `PARTIAL_FAILED` 可以触发；`FAILED` / `NOOP` 不触发。
9. Auto Trigger 只为来源 Analysis Batch 的 Owner 创建 Recommendation Run。
10. Recommendation Profile 显式绑定一个 AI Prompt Profile；只有来源 Batch 使用同一个 Prompt Profile 时才自动刷新。
11. Recommendation Algorithm V1：
   - AI relevance：70%
   - structured profile match：20%
   - freshness：10%
12. V1 不使用 Embedding、Vector DB、RAG、ML Learning-to-Rank。
13. Feedback 与求职处理状态分离：
   - `feedbackState`: NONE / INTERESTED / NOT_INTERESTED
   - `jobDisposition`: NONE / CONTACTED / CONTACTED_NOT_SUITABLE
14. `NOT_INTERESTED` 与 `CONTACTED_NOT_SUITABLE` 都是 Recommendation hard exclusion。
15. `CONTACTED` 只表示已联系/正在沟通，不自动排除。
16. 当前 Feed 查询也会隐藏 hard-excluded Information，因此用户标记后无需等待下一轮刷新即可从推荐列表消失。
17. hard exclusion 按平台 `informationId` 生效，跨 Snapshot、跨 Recommendation Run 持续有效。
18. Phase 4 不自动读取 BOSS 聊天记录；联系状态由用户手动维护。
19. Phase 4 开始先完成 Backend Operational Logging Baseline。
20. Phase 4 Web 只实现推荐功能，不做全站 UI/UX 重构。
21. UI/逻辑整体优化留给下一阶段；生产部署与公网发布继续后置。

## 4. 文件清单

核心：

- `docs/PHASE4_SCOPE.md`
- `docs/PHASE4_ARCHITECTURE_DRAFT.md`
- `docs/PHASE4_DATA_MODEL_DRAFT.md`
- `docs/DATABASE_DESIGN_PHASE4_DRAFT.md`
- `docs/LOGGING_CONVENTIONS.md`
- `docs/CODEX_PHASE4_WORKFLOW.md`
- `docs/PHASE4_EXISTING_DOC_CHANGES.md`

ADR：

- ADR-014：Precomputed Recommendation
- ADR-015：Recommendation Trigger Lifecycle
- ADR-016：Profile / Feedback / Job Disposition Boundary
- ADR-017：Backend Operational Logging
- ADR-018：Recommendation Generic Core + Domain Extension

Contracts：

- Recommendation Profile V1
- Recommendation Refresh V1
- Recommendation Feed V1
- Recommendation Interaction V1

TASK：

- TASK-033 ～ TASK-044

## 5. 实施顺序

```text
TASK-033 Logging Baseline
        ↓
TASK-034 DB / Flyway
↓
TASK-034A Recommendation Model Generalization
↓
TASK-035 Recommendation Profile
        ↓
TASK-036 Interaction / Job Disposition
        ↓
TASK-037 Candidate Resolver
        ↓
TASK-038 Scoring / Explainability
        ↓
TASK-039 Ranking / Dedup / Diversity
        ↓
TASK-040 Run / Worker / Manual Refresh
        ↓
TASK-041 Analysis Batch Auto Trigger
        ↓
TASK-042 Feed API
        ↓
TASK-043 Phase 4 Web
        ↓
TASK-044 E2E / Phase 4 Closeout
```
