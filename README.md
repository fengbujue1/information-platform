# Information Platform

通用的信息采集、归档、AI 分析、推荐和浏览平台。

## 当前状态

Phase 1、Phase 2、Phase 3、Phase 4 已完成。

Phase 5 — Product Refinement & Stabilization 正在进行，采用 Rolling / Just-in-Time Task Planning。

当前已打通：

```text
BOSS Collector
→ InformationEnvelope V1
→ Information Hub
→ MySQL 幂等归档与历史快照
→ Job Query API V1
→ Information Hub Web
→ Identity / Session / CSRF
→ Prompt Profile / Prompt Version
→ Snapshot 级 Information Analysis
→ Manual / Scheduled Batch
→ OpenAI-compatible Provider
→ Actual Token Usage
→ Phase 3 Web
→ Analysis Batch terminal event
→ Recommendation Run / Item
→ Recommendation Feed / Interaction
→ Phase 4 Web
```

Phase 4 已完成，`TASK-033`～`TASK-044`（含 `TASK-034A` Recommendation Model Generalization）均已验收；当前数据库为 V4 Generic Core + JOB Extension。

## Phase 1：采集与归档

已完成：

- BOSS 职位列表与详情采集；
- InformationEnvelope V1；
- Collector → Information Hub 接入；
- MySQL/Flyway；
- 幂等归档；
- 非破坏性合并；
- 历史 Snapshot；
- Collector Outbox；
- Job Query API；
- 真实端到端验收。

## Phase 2：Web 浏览

已完成：

- Vue 3 Web；
- 职位列表；
- 搜索、筛选、排序、分页；
- 职位详情；
- 历史 Snapshot；
- Session 保护后的 Job Query；
- Browser E2E；
- Nginx 同源部署模板。

## Phase 3：AI Processing Foundation & User Relevance MVP

Phase 3 已完成。

核心能力：

- Identity MVP；
- Session / CSRF / Owner 隔离；
- Prompt Profile；
- immutable Prompt Version；
- Analysis Definition Registry；
- Snapshot Input Projection；
- 平台 System Prompt / Output Schema；
- OpenAI-compatible Provider；
- Actual Provider Usage；
- Prompt Assembly；
- 严格结构化输出校验；
- Single Information Analysis；
- Candidate Resolver；
- 最近 N 天 Preview；
- Token Estimate；
- HMAC Preview Confirm Token；
- Manual Batch；
- Budget Guard；
- MySQL Worker；
- Daily Schedule；
- 用户维度 Actual Token Usage；
- Phase 3 Web；
- Fake Provider Full-stack E2E；
- 小规模真实 Provider 受控联调。

### JOB User Relevance Definition

Phase 3 首个真实 Information Type 为：

```text
informationType = JOB
analysisPurpose = USER_RELEVANCE
analysisDefinitionKey = JOB_USER_RELEVANCE
```

历史版本：

```text
version = 1
maxOutputTokens = 1000
```

真实 Provider 联调后通过兼容版本演进新增：

```text
version = 2
maxOutputTokens = 5000
```

V1 不修改，用于历史 Analysis 和冻结 Batch 的版本解析。

V2 继续复用 V1 的：

- Input Projection；
- System Prompt Version 1；
- Output Schema Version 1；
- Output Validator；
- Source Content Boundary。

新的 Analysis 使用 Registry 当前最高版本。

## 安全默认值

真实 AI Provider 默认关闭。

真实调用必须由服务端显式配置：

```text
INFORMATION_HUB_AI_ENABLED=true
INFORMATION_HUB_AI_BASE_URL
INFORMATION_HUB_AI_API_KEY
INFORMATION_HUB_AI_MODEL
```

Batch Worker 同样需要显式开启。
Recommendation Worker 同样默认关闭；手动刷新只创建 `PENDING` Run，需要显式开启 `INFORMATION_HUB_RECOMMENDATION_WORKER_ENABLED=true` 才会执行。Recommendation 不调用 AI Provider。

API Key、Bootstrap 密码、Session Cookie、Collector Token 和完整 Provider 原始响应不得进入 Git、前端或业务日志。

## Phase 3 明确未做

- 非 JOB 信息类型的真实 AI Definition；
- RAG；
- Embedding；
- Vector DB；
- Elasticsearch；
- Kafka / Redis；
- 自动 Top N 推荐；
- Notification；
- 公共注册；
- RBAC / 多租户；
- 费用账单。

## Phase 4：Personalized Recommendation MVP

状态：**已完成。**

Phase 4 的 Scope、Architecture、Data Model、Database Design、ADR、Contract 和 TASK 已完成评审并进入 Accepted 实施基线。

`TASK-033`～`TASK-044`（含 `TASK-034A`）已完成。当前已落地 Generic Recommendation Core + JOB Extension、Recommendation Profile 与 Interaction API、JOB Candidate Resolver、`JOB_RECOMMENDATION / V1` 确定性评分与排序、Recommendation Run / Worker / Manual Refresh、Analysis Batch AFTER_COMMIT Auto Trigger、Recommendation Feed、Phase 4 Web，以及使用真实后端、真实测试 MySQL 和 Fake Provider 的全栈 E2E。Phase 4 仍只实现 JOB；Notification、Retrieval 和其它 Information Type 推荐不在本阶段范围。

## Phase 5：Product Refinement & Stabilization

状态：**进行中。**

Phase 5 面向已交付产品的持续完善与稳定化，不预先创建完整 TASK 列表；仅在实际问题被确认并达到独立任务标准时，按 Rolling / Just-in-Time 模式创建任务。

主要工作范围：

- 现有 UI / UX 细节优化；
- 后端行为完善与缺陷修复；
- 前后端联调与稳定性提升；
- 测试、CI、安全、可靠性与生产准备；
- 为解决已确认问题所必需的小规模重构。

Phase 5 的任务编号从 `TASK-045` 开始。当前没有 Active TASK，`TASK-045` 尚未创建。Phase 4 继续保持已完成和已归档状态，Phase 5 不重新打开或改变其冻结边界。

## 文档入口

- [文档索引](docs/README.md)
- [项目背景](docs/PROJECT_CONTEXT.md)
- [路线图](docs/ROADMAP.md)
- [当前状态](docs/CURRENT_STATUS.md)
- [Phase 3 Scope](docs/PHASE3_SCOPE.md)
- [Phase 3 Architecture](docs/PHASE3_ARCHITECTURE_DRAFT.md)
- [Phase 3 Data Model](docs/PHASE3_DATA_MODEL_DRAFT.md)
- [Phase 3 完成总结](docs/PHASE3_COMPLETION.md)
- [JOB User Relevance V1](docs/contracts/job-user-relevance-v1.md)
- [JOB User Relevance V2](docs/contracts/job-user-relevance-v2.md)
- [Phase 4 正式规划包](PHASE4_PACKAGE_MANIFEST.md)
- [Phase 4 官方设计摘要](PHASE4_REVIEW_SUMMARY.md)
- [Phase 4 Scope](docs/PHASE4_SCOPE.md)
- [Phase 4 Architecture](docs/PHASE4_ARCHITECTURE_DRAFT.md)
- [Phase 4 Data Model](docs/PHASE4_DATA_MODEL_DRAFT.md)
- [Phase 4 Database Design](docs/DATABASE_DESIGN_PHASE4_DRAFT.md)
- [Phase 4 完成总结](docs/PHASE4_COMPLETION.md)
- [Information Hub 配置说明](backend/information-hub/CONFIGURATION.md)
- [Backend Operational Logging Conventions](docs/LOGGING_CONVENTIONS.md)
- [Codex Phase 4 Workflow](docs/CODEX_PHASE4_WORKFLOW.md)
- [Phase 5 正式规划包](PHASE5_PACKAGE_MANIFEST.md)
- [Phase 5 Scope](docs/PHASE5_SCOPE.md)
- [Phase 5 Task Model](docs/PHASE5_TASK_MODEL.md)
- [Phase 5 Task Index](docs/PHASE5_TASK_INDEX.md)

- [Phase 5 Task Template](docs/PHASE5_TASK_TEMPLATE.md)
- [Codex Phase 5 Workflow](docs/CODEX_PHASE5_WORKFLOW.md)
- [Codex Phase 5 Interaction Guide](docs/CODEX_PHASE5_INTERACTION_GUIDE.md)
## 合规说明

采集器仅用于个人学习、技术研究和用户有权访问的数据处理。使用者应遵守目标网站规则、适用法律和合理访问频率。
