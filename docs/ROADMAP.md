# Information Platform Roadmap

## Phase 0：项目初始化

状态：已完成。

## Phase 1：BOSS 采集接入与归档

状态：进行中。

当前步骤：

```text
实际输出结构审计
→ 用户确认字段映射
→ 冻结 InformationEnvelope V1
→ 创建 Spring Boot
→ 建立远程 MySQL 开发与测试环境
→ 创建 Flyway 数据库结构
→ 实现信息接入 API
```

任务：

1. [分析 BOSS 输出结构](tasks/TASK-001.md)
2. [确认 InformationEnvelope V1](tasks/TASK-002.md)
3. [创建 Spring Boot 后端](tasks/TASK-003.md)
4. [建立远程 MySQL 开发与测试环境](tasks/TASK-004A.md)
5. [创建 Phase 1 数据库结构](tasks/TASK-004.md)
6. [实现信息接入 API](tasks/TASK-005.md)
7. [实现 BOSS 字段映射器](tasks/TASK-006.md)
8. [实现 Information Hub Client](tasks/TASK-007.md)
9. [实现本地 Outbox](tasks/TASK-008.md)
10. [实现职位查询 API](tasks/TASK-009.md)
11. [完成端到端验收](tasks/TASK-010.md)

Phase 1 完成标准：

- 列表和详情可以可靠合并。
- sourceItemId 使用 encrypt_job_id。
- 后端不可用时数据不丢失。
- 重复提交不重复创建当前记录。
- 内容变化创建递增版本快照。
- 临时详情失败不清空旧 JD。
- security_id、lid 和凭证不进入中央数据库。
- 可以查询当前职位和历史版本。
- 测试通过。

## Phase 2：Web 浏览

状态：未开始。

- 职位列表与详情；
- 搜索和筛选；
- 原始业务数据查看；
- 历史版本查看。

## Phase 3：AI 分析

状态：未开始。

来源标签和 AI 标准技能必须分开保存。
