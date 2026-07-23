# Information Platform

通用的信息采集、归档、分析、推荐和浏览平台。

## 当前状态

当前处于 Phase 1：BOSS 职位数据接入与历史归档。

当前正在确认：

- BOSS 列表和详情字段映射；
- InformationEnvelope V1；
- Phase 1 数据库结构。

## Phase 1 核心链路

```text
BOSS 列表与详情
→ Collector 适配层合并
→ Information Hub
→ MySQL 当前版本与历史快照
→ Query API
```

## 仓库结构

- `collectors`：独立采集器
- `backend`：Spring Boot 信息中枢
- `frontend`：Vue 信息浏览前端
- `deploy`：Docker Compose
- `docs`：架构、协议、任务和决策

## 文档入口

- [文档索引](docs/README.md)
- [数据库设计](docs/DATABASE_DESIGN.md)
- [InformationEnvelope V1](docs/contracts/information-envelope-v1.md)
- [BOSS 字段映射](docs/contracts/boss-job-field-mapping.md)
- [当前状态](docs/CURRENT_STATUS.md)

## 当前限制

当前不引入微服务、Kafka、MongoDB、Elasticsearch 或 Kubernetes。

## 合规说明

采集器仅用于个人学习、技术研究和用户有权访问的数据处理。使用者应遵守目标网站规则、适用法律和合理访问频率。
