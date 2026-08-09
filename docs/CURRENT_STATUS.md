# 当前开发状态

更新时间：2026-08-09
当前分支：dev

## 当前阶段

- Phase 0：已完成。
- Phase 1：已完成。
- Phase 2：已完成。
- Phase 3：已完成。
- Phase 4：进行中。

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

`TASK-036`：Recommendation Interaction / Feedback / Job Disposition（尚未实施）。

`TASK-033`、`TASK-034`、数据模型通用化纠偏 `TASK-034A` 与 `TASK-035` Recommendation Profile Backend/API 已完成；当前仅将实施顺序推进到 TASK-036，不代表 TASK-036 已开始编码。

Phase 3 已归档。后续仅接受缺陷修复、安全修复、依赖维护和运行参数维护，不再在 Phase 3 范围继续增加新功能。

## Phase 4 启动状态

Phase 4：Personalized Recommendation MVP 已启动，规划基线已 Accepted。

已完成：

- Scope / Architecture / Data Model / Database Design Draft；
- ADR-014 ～ ADR-017；
- Recommendation Profile / Refresh / Feed / Interaction Contracts；
- TASK-033 ～ TASK-044 拆分；
- Backend Operational Logging Conventions；
- Codex Phase 4 Workflow；
- TASK-033 Backend Operational Logging Baseline；
- TASK-034 Recommendation Data Model / Flyway（V3）；
- TASK-034A Generic Recommendation Core + JOB Domain Extension（V4）。
- TASK-035 Recommendation Profile Domain And API。

尚未实施：

- Interaction、Candidate、Scoring、Ranking、Run、Trigger、Feed；
- Phase 4 Web 与 E2E。

## CI

CI 后续作为独立工程维护事项处理，不作为本次 Phase 3 阶段关闭的阻塞条件。

## 下一步

按 Accepted Phase 4 规划实施 `TASK-036`；不得提前实施后续 Recommendation 业务任务。
