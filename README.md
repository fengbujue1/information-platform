# Information Platform

通用的信息采集、归档、分析、推荐和浏览平台。

## 当前状态

Phase 1 和 Phase 2 已完成。

当前已打通：

```text
BOSS Collector
→ 列表与详情合并
→ InformationEnvelope V1
→ Information Hub
→ MySQL 幂等归档与历史快照
→ Job Query API V1
→ Information Hub Web
```

Phase 2 已完成 Vue 3 标准化职位浏览 MVP、基础 CI、受控 Web 构建部署和端到端验收。项目当前等待 Phase 3 范围规划，不提前实施 AI、推荐或通知。

## 当前可用能力

- BOSS 职位列表与详情采集；
- 统一协议接入 Information Hub；
- MySQL 幂等归档、非破坏性合并和历史快照；
- 职位列表、详情和快照只读 API；
- 职位搜索、筛选、排序和分页；
- 职位详情与历史版本浏览；
- URL 状态恢复、Loading、Empty、Error 和 404；
- Java、Python、Vue 和浏览器 E2E 持续集成；
- Nginx SPA 回退和 `/api` 同源代理受控部署模板。

## Phase 2 边界

Phase 2 不包含：

- rawPayload 查看；
- 用户注册和登录；
- 复杂权限；
- 职位编辑和删除；
- AI 分析；
- 个性化推荐；
- 消息通知；
- 公网无认证部署。

## 仓库结构

- `collectors`：独立采集器
- `backend`：Spring Boot Information Hub
- `frontend`：Vue Information Hub Web
- `deploy`：Docker Compose 和部署说明
- `docs`：架构、协议、任务和决策

## 文档入口

- [文档索引](docs/README.md)
- [项目背景](docs/PROJECT_CONTEXT.md)
- [架构](docs/ARCHITECTURE.md)
- [Phase 2 范围](docs/PHASE2_SCOPE.md)
- [路线图](docs/ROADMAP.md)
- [当前状态](docs/CURRENT_STATUS.md)
- [Job Query API V1](docs/contracts/job-query-api-v1.md)
- [Web UI Behavior V1](docs/contracts/web-ui-behavior-v1.md)

## 当前限制

当前不引入微服务、Kafka、MongoDB、Elasticsearch、向量数据库或 Kubernetes。Phase 3 必须先完成范围、数据模型、安全边界和成本约束设计，再开始 AI 业务代码。

## 合规说明

采集器仅用于个人学习、技术研究和用户有权访问的数据处理。使用者应遵守目标网站规则、适用法律和合理访问频率。
