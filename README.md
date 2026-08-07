# Information Platform

通用的信息采集、归档、AI 分析、推荐和浏览平台。

## 当前状态

Phase 1、Phase 2、Phase 3 已完成。

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
```

Phase 4 尚未启动。

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

## 下一阶段

Phase 4：个性化推荐。

当前状态：**未开始**。

Phase 4 必须重新完成 Scope、ADR、Contract 和 TASK 规划后再进入实施。

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

## 合规说明

采集器仅用于个人学习、技术研究和用户有权访问的数据处理。使用者应遵守目标网站规则、适用法律和合理访问频率。
