# 当前开发状态

更新时间：2026-07-24
当前分支：dev

## 当前阶段

Phase 1：BOSS 采集接入与归档。

## 已完成

- 创建 Monorepo 和模块级 AGENTS。
- 导入并验证 BOSS Collector。
- 建立数据库、协议、TASK 和 ADR 草案。
- 获取并分析真实 BOSS 列表与详情 JSON。
- 确认 90 条列表和 25 条详情的数据结构。
- 确认详情使用 job_id 与列表关联。
- 确认平台 sourceItemId 应使用 encrypt_job_id。
- 确认 boss_name 是公司名，encrypt_boss_id 是招聘者来源 ID。
- 确认 job_labels 与 tags 在当前样本完全重复。
- 确认 details.skill_tags 当前不适合作为标准技能。
- 完成 TASK-001 BOSS 输出结构审计。
- 完成 TASK-002 并冻结 InformationEnvelope V1。
- 接受数据库设计 1.3 和 ADR-007。
- 完成 TASK-003，创建可编译、测试和启动的 Information Hub Spring Boot 项目骨架。
- 完成 TASK-004A，建立远程 Docker MySQL 开发与集成测试环境，并通过安全、权限、持久化和备份恢复验证。
- 接受 ADR-008，确定使用远程 Docker MySQL 作为共享开发与集成测试数据库，并通过 SSH 隧道安全连接。

## 当前任务

TASK-004：创建 Phase 1 数据库结构。

## 已冻结设计

- 平台 `sourceItemId` 使用 `encrypt_job_id`，`job_id` 只用于 Collector 内部合并。
- `boss_name` 映射 `companyName`，`encrypt_boss_id` 映射 `sourceRecruiterId`。
- `security_id` 和 `lid` 在提交 Information Hub 前删除。
- 历史无时区时间按 `Asia/Shanghai` 解释；新采集时间必须带明确时区。
- `detailStatus` 支持 `UNKNOWN`、`FETCHED`、`FAILED`、`UNAVAILABLE`。
- `tags` 规则识别失败时仅保留 `sourceTags`。
- 快照使用递增 `versionNo` 并支持 `A → B → A`。
- 更新采用非破坏性合并，null 或空值不覆盖已有有效值。
- `information_snapshot` 外键采用 `ON DELETE RESTRICT`。
- 开发与集成测试使用远程 Docker MySQL，生产数据库保持独立。
- MySQL 不向公网直接开放 `3306`，开发主机通过 SSH 隧道连接。
- 开发库与测试库分离，并使用不同的非 root 最小权限账号。

## 下一步

执行 TASK-004，通过 Flyway 创建 Phase 1 数据库结构，并使用独立测试库完成迁移验证。
