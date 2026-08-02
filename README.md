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

Phase 3 正在进行设计冻结，尚未开始 AI 业务代码。

## Phase 3 Draft

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
- Vue 3 职位搜索、筛选、排序、分页和历史查看；
- Java、Python、Vue 和浏览器 E2E CI；
- Nginx SPA 回退和 `/api` 同源代理受控部署模板。

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
- [Phase 3 Architecture Draft](docs/PHASE3_ARCHITECTURE_DRAFT.md)
- [Phase 3 Data Model Draft](docs/PHASE3_DATA_MODEL_DRAFT.md)
- [Phase 3 Codex Workflow](docs/CODEX_PHASE3_WORKFLOW.md)
- [路线图](docs/ROADMAP.md)
- [当前状态](docs/CURRENT_STATUS.md)

## 合规说明

采集器仅用于个人学习、技术研究和用户有权访问的数据处理。使用者应遵守目标网站规则、适用法律和合理访问频率。
