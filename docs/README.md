# Information Platform 文档索引

## 项目与架构

- [项目背景](PROJECT_CONTEXT.md)
- [总体架构](ARCHITECTURE.md)
- [开发路线图](ROADMAP.md)
- [当前开发状态](CURRENT_STATUS.md)
- [数据库设计](DATABASE_DESIGN.md)
- [Codex 协作流程](CODEX_WORKFLOW.md)
- [上游仓库记录](UPSTREAMS.md)

## 接口协议

- [InformationEnvelope V1](contracts/information-envelope-v1.md)

## 架构决策

- [ADR-001：Monorepo](decisions/ADR-001-use-monorepo.md)
- [ADR-002：MySQL](decisions/ADR-002-use-mysql.md)
- [ADR-003：模块化单体](decisions/ADR-003-use-modular-monolith.md)
- [ADR-004：Collector HTTP 接入](decisions/ADR-004-collector-http-integration.md)
- [ADR-005：UTC 时间与业务内容 Hash](decisions/ADR-005-time-and-content-hash.md)

## Phase 1 任务

1. [TASK-001：分析 BOSS 输出结构](tasks/TASK-001.md)
2. [TASK-002：确认 InformationEnvelope V1](tasks/TASK-002.md)
3. [TASK-003：创建 Spring Boot 后端](tasks/TASK-003.md)
4. [TASK-004：创建数据库结构](tasks/TASK-004.md)
5. [TASK-005：实现接入 API](tasks/TASK-005.md)
6. [TASK-006：实现 BOSS 字段映射器](tasks/TASK-006.md)
7. [TASK-007：实现 Information Hub Client](tasks/TASK-007.md)
8. [TASK-008：实现本地 Outbox](tasks/TASK-008.md)
9. [TASK-009：实现职位查询 API](tasks/TASK-009.md)
10. [TASK-010：Phase 1 端到端验收](tasks/TASK-010.md)
