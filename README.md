# Information Platform

通用的信息采集、归档、分析、推荐和浏览平台。

## 当前状态

Phase 1 和 Phase 2 已完成。

当前已打通：

```text
BOSS Collector
→ InformationEnvelope V1
→ Information Hub
→ MySQL 幂等归档与历史快照
→ Job Query API V1
→ Information Hub Web
```

Phase 3 设计已冻结。数据库基础、Identity、Prompt Version、Analysis Definition、Provider、Prompt Assembly/结构化输出、单条 Information Analysis、Candidate Resolver/Preview/Token Estimate、异步 Analysis Batch/Budget Guard，以及每日 Analysis Schedule 已完成；当前下一实施任务是 TASK-031 Phase 3 Web。

## Phase 3 Accepted Design

Phase 3 计划建设：

```text
AI Processing Foundation
+
User Relevance MVP
```

核心方向：

- 最小登录身份；
- 账号级 Prompt Profile；
- Prompt Version；
- Snapshot 级 Information Analysis；
- 通用 Analysis Definition；
- OpenAI-compatible Provider；
- Token Estimate 与 Actual Usage；
- 最近 N 天 Preview；
- 异步 Batch；
- 每日 Schedule；
- Phase 3 Web。

第一版只实现 JOB + USER_RELEVANCE，但 AI 核心命名和模型保持通用 Information 语义。

## 当前可用能力

- BOSS 职位列表与详情采集；
- 统一协议接入 Information Hub；
- MySQL 幂等归档、非破坏性合并和历史快照；
- 职位列表、详情和快照只读 API；
- 需要 Session + CSRF 的单条 Analysis 执行 API，以及按 Owner 隔离的 Analysis 查询 API；
- 受控账号 Bootstrap、安全密码摘要、同源 Session、CSRF、登录/登出和当前用户接口；
- 需要 Session 的 Job Query API，以及 Vue 3 登录、职位搜索、筛选、排序、分页和历史查看；
- 保持独立 Bearer Token 认证的 Collector 接口；
- 需要 Session + CSRF 的账号级 Prompt Profile / Version API；
- Java、Python、Vue 和浏览器 E2E CI；
- Nginx SPA 回退和 `/api` 同源代理受控部署模板。

启动 Identity Bootstrap 时，通过部署环境显式提供
`INFORMATION_HUB_BOOTSTRAP_USERNAME`、`INFORMATION_HUB_BOOTSTRAP_PASSWORD`、
`INFORMATION_HUB_BOOTSTRAP_DISPLAY_NAME` 和 `INFORMATION_HUB_BOOTSTRAP_TIMEZONE`。
生产 HTTPS 环境应设置 `INFORMATION_HUB_SESSION_COOKIE_SECURE=true`。仓库不提供默认密码，也不保存真实凭据。

真实 AI 默认关闭。仅在服务端显式提供 `INFORMATION_HUB_AI_ENABLED=true`、
`INFORMATION_HUB_AI_BASE_URL`、`INFORMATION_HUB_AI_API_KEY` 和
`INFORMATION_HUB_AI_MODEL` 后才允许调用 OpenAI-compatible Provider；可选配置
`INFORMATION_HUB_AI_TIMEOUT` 和 `INFORMATION_HUB_AI_MAX_OUTPUT_TOKENS`。API Key
不得进入前端、数据库业务表、Git 或日志。

## Phase 3 明确不做

- 其他信息类型的实际 AI 实现；
- RAG；
- Embedding；
- 向量数据库；
- Elasticsearch；
- Kafka / Redis；
- 自动 Top N 推荐；
- 通知；
- 复杂用户权限；
- 公网无保护开放。

## 文档入口

- [文档索引](docs/README.md)
- [Phase 3 Scope](docs/PHASE3_SCOPE.md)
- [Phase 3 Architecture（Accepted）](docs/PHASE3_ARCHITECTURE_DRAFT.md)
- [Phase 3 Data Model（Accepted）](docs/PHASE3_DATA_MODEL_DRAFT.md)
- [Phase 3 Codex Workflow](docs/CODEX_PHASE3_WORKFLOW.md)
- [路线图](docs/ROADMAP.md)
- [当前状态](docs/CURRENT_STATUS.md)

## 合规说明

采集器仅用于个人学习、技术研究和用户有权访问的数据处理。使用者应遵守目标网站规则、适用法律和合理访问频率。
