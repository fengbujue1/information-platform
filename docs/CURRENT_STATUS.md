# 当前开发状态

更新时间：2026-07-23  
当前分支：dev

## 当前阶段

Phase 1：采集接入与归档。

## 已完成

- 创建 Monorepo。
- 导入并验证 BOSS 采集器。
- 创建项目级和模块级 AGENTS。
- 创建架构、数据库、协议、TASK 和 ADR 文档。
- 识别 BOSS 当前主要字段及若干字段含义问题。

## 当前任务

`TASK-001`：分析 BOSS 采集器实际输出结构，并确认字段映射。

## 当前设计补充

- 重复提交使用非破坏性更新。
- `information_item.status` 与 `job_information.job_status` 分离。
- Phase 1 保存首次版本及内容变化后的 `information_snapshot`。
- 请求体上限建议为 5 MiB。
- Job 查询 API 使用分页和排序字段白名单。

## 尚未完成

- 完成 TASK-001 字段审计。
- 确认 InformationEnvelope V1。
- 创建 Spring Boot 后端。
- 创建三张 Phase 1 数据表。
- 实现接入 API、Mapper、Client 和 Outbox。
- 实现 Job Query API。
- 完成端到端验收。

## 下一步最小任务

1. 让 Codex 实施 `docs/tasks/TASK-001.md`。
2. 根据实际代码生成字段映射表。
3. 用户确认映射后执行 TASK-002。
