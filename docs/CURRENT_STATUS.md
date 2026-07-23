# 当前开发状态

更新时间：2026-07-23  
当前分支：dev

## 当前阶段

Phase 1：采集接入。

## 已完成

- 创建 Information Platform Monorepo。
- 通过 Git subtree 导入 BOSS 采集器。
- 创建根目录和模块级 `AGENTS.md`。
- 创建 `PROJECT_CONTEXT.md`、`ARCHITECTURE.md`、`ROADMAP.md`。
- 创建数据库设计草案、接入协议草案、TASK 和 ADR。
- 添加 `.editorconfig`、`.gitattributes` 和 Monorepo `.gitignore`。
- 验证 BOSS 采集器在 Windows 上能够采集职位列表和详情。
- 从 Git 最新版本中排除采集结果目录。

## 当前任务

`TASK-001`：分析 BOSS 采集器实际输出结构，并确认字段映射。

## 当前已发现的问题

1. BOSS 列表输出中的 `boss_name` 实际来自 `brandName`，表示公司品牌名，不是招聘者姓名。
2. 当前列表输出没有稳定的招聘者姓名和活跃状态字段。
3. `tags` 同时包含经验和学历，需要确认解析策略。
4. `skills`、`welfare` 和 `job_labels` 当前是使用 ` | ` 拼接的字符串。
5. BOSS 来源真实发布时间当前没有可靠字段，应保持 `publishTime = null`。
6. `INTEGRATION.md` 中 Python 版本需要通过本机命令重新确认。

## 尚未完成

- 完成 TASK-001 字段审计和脱敏样例。
- 确认 InformationEnvelope V1。
- 创建 Spring Boot 后端。
- 创建 Flyway 数据库结构。
- 实现采集接入 API。
- 实现 BOSS Mapper、Hub Client 和 Outbox。
- 实现职位查询 API。
- 完成 Phase 1 端到端验收。

## 下一步最小任务

1. 让 Codex 实施 `docs/tasks/TASK-001.md`。
2. 根据实际代码生成字段映射表。
3. 用户确认映射后，将 TASK-001 标记为完成并开始 TASK-002。
