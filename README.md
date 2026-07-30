# Information Platform

通用的信息采集、归档、分析、推荐和浏览平台。

## 当前状态

Phase 1 已完成。

已完成链路：

```text
BOSS Collector
→ 列表与详情合并
→ InformationEnvelope V1
→ Information Hub
→ MySQL 幂等归档与历史快照
→ Job Query API
```

Phase 2 范围已经冻结，TASK-011 至 TASK-015 已完成。

当前任务为 TASK-016：实现历史快照查看。Vue 3 项目骨架、强类型 Job Query API Client、职位列表页和职位详情页已经完成。

## Phase 2 目标

Phase 2 将基于现有 Job Query API 建设 Vue 3 Web 前端，支持：

- 职位列表；
- 搜索、筛选、排序和分页；
- 职位详情；
- 历史快照查看；
- 加载、空数据和错误状态；
- 本地开发与受控环境构建验证。

Phase 2 暂不包含：

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
- [Phase 2 范围](docs/PHASE2_SCOPE.md)
- [Phase 2 Codex 协作流程](docs/CODEX_PHASE2_WORKFLOW.md)
- [路线图](docs/ROADMAP.md)
- [当前状态](docs/CURRENT_STATUS.md)
- [Job Query API V1](docs/contracts/job-query-api-v1.md)

## 当前限制

当前不引入微服务、Kafka、MongoDB、Elasticsearch、向量数据库或 Kubernetes。

## 合规说明

采集器仅用于个人学习、技术研究和用户有权访问的数据处理。使用者应遵守目标网站规则、适用法律和合理访问频率。
