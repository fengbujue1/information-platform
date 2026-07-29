# Information Platform Roadmap

## Phase 0：项目初始化

状态：已完成。

## Phase 1：BOSS 采集接入与归档

状态：已完成。

完成链路：

```text
实际输出结构审计
→ 冻结 InformationEnvelope V1
→ 创建 Spring Boot
→ 建立远程 MySQL 开发与测试环境
→ 创建 Flyway 数据库结构
→ 实现信息接入 API
→ 完成 BOSS Mapper
→ 完成 Information Hub Client
→ 完成本地 Outbox
→ 实现职位查询 API
→ 完成真实端到端验收
```

Phase 1 任务：

1. [TASK-001：分析 BOSS 输出结构](tasks/TASK-001.md)
2. [TASK-002：确认 InformationEnvelope V1](tasks/TASK-002.md)
3. [TASK-003：创建 Spring Boot 后端](tasks/TASK-003.md)
4. [TASK-004A：建立远程 MySQL 开发与测试环境](tasks/TASK-004A.md)
5. [TASK-004：创建数据库结构](tasks/TASK-004.md)
6. [TASK-005：实现接入 API](tasks/TASK-005.md)
7. [TASK-006A：增加原始响应诊断输出](tasks/TASK-006A.md)
8. [TASK-006B：接入招聘者在线观测时间](tasks/TASK-006B.md)
9. [TASK-006：实现 BOSS 字段映射器](tasks/TASK-006.md)
10. [TASK-007：实现 Information Hub Client](tasks/TASK-007.md)
11. [TASK-007A：增加外部配置文件](tasks/TASK-007A.md)
12. [TASK-007B：修复任意工作目录启动](tasks/TASK-007B.md)
13. [TASK-008：实现本地 Outbox](tasks/TASK-008.md)
14. [TASK-009：实现职位查询 API](tasks/TASK-009.md)
15. [TASK-010：完成端到端验收](tasks/TASK-010.md)

## Phase 2：标准化职位 Web 浏览 MVP

状态：规划中。

范围文档：

- [Phase 2 范围](PHASE2_SCOPE.md)
- [Web UI Behavior V1](contracts/web-ui-behavior-v1.md)
- [ADR-010：Phase 2 Web MVP 边界](decisions/ADR-010-phase2-web-mvp-boundary.md)

目标链路：

```text
浏览器
→ Information Hub Web
→ Job Query API V1
→ Information Hub
→ MySQL
```

Phase 2 任务：

1. [TASK-011：冻结 Phase 2 Web MVP 范围](tasks/TASK-011.md)
2. [TASK-012：创建 Vue 3 项目骨架](tasks/TASK-012.md)
3. [TASK-013：实现 API Client、类型与环境配置](tasks/TASK-013.md)
4. [TASK-014：实现职位列表、筛选和分页](tasks/TASK-014.md)
5. [TASK-015：实现职位详情](tasks/TASK-015.md)
6. [TASK-016：实现历史快照查看](tasks/TASK-016.md)
7. [TASK-017：完善交互、响应式和健壮性](tasks/TASK-017.md)
8. [TASK-018：建立 CI 与受控构建部署配置](tasks/TASK-018.md)
9. [TASK-019：完成 Phase 2 端到端验收](tasks/TASK-019.md)

Phase 2 不包含：

- rawPayload 管理；
- 用户系统；
- 写操作；
- AI；
- 推荐；
- 通知；
- 公网无认证部署。

## Phase 3：AI 分析

状态：未开始。

原则：

- 来源标签与 AI 标准技能分开保存。
- AI 分析结果不覆盖原始信息。
- 内容质量分与用户匹配分分离。
- 模型、Prompt、Token 和分析版本可追溯。

## Phase 4：个性化推荐与通知

状态：未开始。

可能包含：

- 用户画像；
- 职位匹配评分；
- 相似职位去重；
- 公司多样性；
- 每日 Top N；
- Web 通知；
- 其他受控通知渠道。
