# 当前开发状态

更新时间：2026-08-14
当前分支：dev

## 当前阶段

- Phase 0：已完成。
- Phase 1：已完成。
- Phase 2：已完成。
- Phase 3：已完成。
- Phase 4：已完成。
- Phase 5：进行中。

## Phase 3 完成情况

Phase 3：AI Processing Foundation & User Relevance MVP 已完成。

已完成能力：

- Phase 3 Scope / Architecture / Data Model / Database Design / ADR / Contracts；
- Identity MVP；
- Session / CSRF / Owner 隔离；
- Prompt Profile / immutable Prompt Version；
- Analysis Definition Registry；
- JOB User Relevance Definition；
- Snapshot Input Projection；
- 平台 System Prompt / Output Schema；
- OpenAI-compatible Provider；
- Provider Usage；
- Prompt Assembly；
- strict structured output validation；
- Single Information Analysis；
- Invocation attempt / Actual Usage；
- Candidate Resolver；
- 最近 N 天 Preview；
- Token Estimate；
- HMAC Preview Confirm Token；
- Manual Batch；
- Budget Guard；
- MySQL Worker；
- Daily Schedule；
- Schedule Dispatcher；
- Phase 3 Web；
- 用户 Actual Token Usage；
- Fake Provider Full-stack E2E；
- 真实 Provider 受控联调；
- Phase 3 文档收尾。

## 当前完整业务链

```text
BOSS Collector
→ InformationEnvelope V1
→ Information Hub
→ MySQL Current + Snapshot
→ Job Query API
→ Web

User
→ Login / Session / CSRF
→ Prompt Profile / Version
→ Snapshot
→ Analysis / Preview
→ Manual or Scheduled Batch
→ AI Provider
→ Structured Analysis Result
→ Model Invocation Actual Usage
→ Web
→ Analysis Batch terminal event
→ Recommendation Run / Item
→ Recommendation Feed / Interaction
→ Phase 4 Web
```

## Analysis Definition 当前版本

历史 V1：

```text
analysisDefinitionKey = JOB_USER_RELEVANCE
analysisDefinitionVersion = 1
maxOutputTokens = 1000
```

当前 V2：

```text
analysisDefinitionKey = JOB_USER_RELEVANCE
analysisDefinitionVersion = 2
maxOutputTokens = 5000
```

V1 保持不可变。

V2 继续复用 V1：

- Input Projection；
- System Prompt Version 1；
- Output Schema Version 1；
- Validator；
- Source Content Boundary。

新的 Analysis 使用 Registry 当前最高版本 V2。

## 安全与运行边界

- Provider 默认关闭；
- Batch Worker 默认关闭；
- Recommendation Worker 默认关闭；手动或自动创建的 `PENDING` Run 只有在显式启用 Worker 后才会执行，且 Recommendation 不调用 Provider；
- Schedule 默认关闭；
- API Key 只来自服务端；
- Actual Token 只来自 Provider Usage；
- Provider HTTP 在数据库事务之外；
- timeout / ambiguous failure 不自动盲重试；
- Preview 不持久化；
- Collector Bearer Token 与用户 Session 分离；
- rawPayload 不默认进入模型；
- AI 结果不覆盖来源事实。

## Phase 3 持久化表

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

## 当前任务

`TASK-048` 已完成 `v0.1.0-beta.1` Baseline Preparation；本地验证与 GitHub CI release gate 均已通过，状态为 `DONE`，已知 Baseline blocker 已全部清除。

当前没有 Active / VERIFYING / DEFERRED TASK；下一个任务编号为 `TASK-049`。任务状态与编号以 [Phase 5 Task Index](PHASE5_TASK_INDEX.md) 为准。

`TASK-033`～`TASK-044`（含数据模型通用化纠偏 `TASK-034A`）均已完成，Phase 4 保持已完成和已归档状态。

Phase 3 已归档。后续仅接受缺陷修复、安全修复、依赖维护和运行参数维护，不再在 Phase 3 范围继续增加新功能。

## Phase 4 完成状态

Phase 4：Personalized Recommendation MVP 已完成并归档。

已完成：

- Scope / Architecture / Data Model / Database Design Draft；
- ADR-014 ～ ADR-018；
- Recommendation Profile / Refresh / Feed / Interaction Contracts；
- TASK-033 ～ TASK-044 拆分；
- Backend Operational Logging Conventions；
- Codex Phase 4 Workflow；
- TASK-033 Backend Operational Logging Baseline；
- TASK-034 Recommendation Data Model / Flyway（V3）；
- TASK-034A Generic Recommendation Core + JOB Domain Extension（V4）。
- TASK-035 Recommendation Profile Domain And API。
- TASK-036 Recommendation Interaction / Feedback / Job Disposition。
- TASK-037 Recommendation Candidate Resolver。
- TASK-038 Recommendation Scoring 与 Explainability。
- TASK-039 Ranking、Deduplication、Diversity 与 Top N。
- TASK-040 Recommendation Run、Worker、Manual Refresh 与 Run 状态 API。
- TASK-041 Analysis Batch Completion → Recommendation Auto Trigger。
- TASK-042 Recommendation Feed Query API。
- TASK-043 Phase 4 Web（Profile、Feed、Refresh、Feedback、JOB Contact Status 与本地 Fixture Browser E2E）。
- TASK-044 真实后端 + 测试 MySQL + Fake Provider + Browser 全栈 E2E、最终验收与文档收尾。

验收事实：

- `COMPLETED` 与 `PARTIAL_FAILED` Analysis Batch 均可触发 Auto Recommendation；
- Manual Refresh 不调用 AI Provider；
- Feed、score/reasons、Viewed、Feedback、CONTACTED、hard exclusion、下一轮排除、reset NONE 与 Profile stale 已覆盖；
- FAILED Recommendation Run 保留旧成功 Feed 由真实 MySQL 集成测试覆盖；
- V1～V4 Migration 未修改，当前数据库版本仍为 V4；
- Phase 4 只实际实现 JOB，Notification、Retrieval/Embedding 与其它 Information Type 推荐仍未实施。

## Phase 5 进行状态

Phase 5：Product Refinement & Stabilization 已进入进行中状态，采用 Rolling / Just-in-Time Task Planning。

当前执行事实：

- `TASK-045` 已完成 Phase 3 AI 页面域及推荐画像复用术语的中文化，自动化测试、Fixture Browser E2E 与人工验证均已通过，状态为 `DONE`；
- `TASK-046` 已统一浏览器操作反馈与公共 API 中文错误消息，自动化验证与人工验收均已通过，状态为 `DONE`；
- `TASK-047` 已将 Manual / Schedule 的候选窗口、候选数量和预估 Token 预算改为统一服务端配置，并提供认证只读限制 API；Web 已使用动态默认值与上限，超限旧 Schedule 会明确提示并阻止新执行，自动化检查与人工验收均已完成，状态为 `DONE`；
- `TASK-048` 已完成测试断言与 V3 Migration 跨平台校验修复、Baseline 组件版本和发布文档对齐，以及自包含 Backend MySQL CI 与 Repository checks 稳定化；本地门禁和 GitHub CI release gate 六项 Job 均已通过，状态为 `DONE`；
- 当前没有 Active / VERIFYING / DEFERRED TASK；
- 下一个任务编号为 `TASK-049`；
- 不预先创建空的后续 TASK 或完整任务列表；
- 仅针对已确认的产品完善、缺陷、联调、稳定性、测试、CI、安全、可靠性或生产准备问题创建任务；
- Phase 4 的完成结论与冻结边界保持不变。

## Baseline 版本策略

- Git Repository / GitHub Release：`v0.1.0-beta.1`，作为整个 information-platform 仓库的首个正式 Baseline；
- Backend `information-hub`：`0.1.0-beta.1`；
- Frontend `information-hub-web`：`0.1.0-beta.1`；
- Collector `boss-zhipin-scraper`：保持独立组件版本 `2.1.0`；
- Flyway Schema Version：保持 `V4`，不与应用发布版本联动。

## CI

TASK-048 本地 Backend 276 tests 与 package、Frontend typecheck / 95 tests / build、Collector 144 tests、Phase 4 Full-stack E2E、Flyway V1～V4 和 `git diff --check` 均已通过。

GitHub CI release gate：`PASS`。

- Repository checks：`PASS`；
- Backend tests：`PASS`；
- Collector tests：`PASS`；
- Frontend checks：`PASS`；
- Phase 3 full-stack E2E：`PASS`；
- Web deployment configuration：`PASS`。

最终 candidate commit 与对应 CI Run 由发布前 Release Validation 动态读取 GitHub 实际状态，不在版本库文档中固化 Run 编号；`v0.1.0-beta.1` Baseline Preparation 的 CI 门禁与已知 blocker 已全部闭环。

## 下一步

Phase 5 保持进行中，等待确认新的独立问题后使用 `TASK-049`。`v0.1.0-beta.1` Baseline Preparation 已完成，但 Git Tag 与 GitHub Release 尚未创建；后续 `dev` → `main` → tag → release 流程须由用户另行确认。
