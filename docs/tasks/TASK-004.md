# TASK-004：创建 Phase 1 数据库结构

状态：DONE
所属阶段：Phase 1  
优先级：P0

## 前置依赖

- TASK-002 完成。
- TASK-003 完成。
- TASK-004A 完成。
- DATABASE_DESIGN 1.3 已确认。

## 目标

通过 Flyway 创建：

- information_item
- job_information
- information_snapshot

## 关键字段

information_item：

- current_version_no

job_information：

- source_recruiter_id
- salary_source
- detail_status
- detail_collected_at
- source_tags
- source_skill_tags

information_snapshot：

- version_no

## 索引要求

- 当前信息幂等唯一索引。
- information_id + version_no 快照唯一索引。
- information_id + content_hash 普通索引。
- source_company_id 和 source_recruiter_id 索引。
- 城市、薪资和状态查询索引。

## 测试

- 使用 TASK-004A 提供的独立集成测试库，不连接或清理共享开发库。
- 三张表迁移。
- JSON 字段读写。
- encrypt ID 中包含特殊字符。
- 快照 A → B → A。
- 同版本号唯一约束。
- 外键和事务回滚。

## 不在范围

- 不实现 Controller。
- 不创建 AI 表。

## 实施记录

完成时间：2026-07-26

实施内容：

- 在 Information Hub 中加入 Spring JDBC、Flyway MySQL 和 MySQL Connector/J 依赖。
- 新增 Flyway V1 迁移，创建 `information_item`、`job_information`、`information_snapshot` 及设计要求的索引和外键。
- `job_information.information_id` 使用 `ON DELETE CASCADE`，`information_snapshot.information_id` 使用 `ON DELETE RESTRICT`。
- 数据库连接参数只通过环境变量注入；仓库未保存真实账号、密码或服务器地址。
- 增加独立测试库集成测试，所有测试写入均在显式 JDBC 事务中执行并回滚，不连接或清理共享开发库。

验证记录：

- Java 21 执行 `./mvnw.cmd test`：通过；Spring Boot 启动测试 1 项通过，未配置测试库时数据库集成测试 6 项按条件跳过。
- 通过 SSH 隧道连接独立测试库执行 `./mvnw.cmd -Dtest=Phase1SchemaMigrationIntegrationTest test`：6 项全部通过。
- 集成测试覆盖三张表与索引、JSON 读写、包含特殊字符的 encrypt ID、快照 `A → B → A`、同版本号唯一约束、当前信息幂等唯一约束、两种外键删除规则和事务回滚。
- 在测试库重复执行 Flyway：V1 校验通过且无重复迁移。
- 通过 Spring Boot/Flyway 将 V1 应用到共享开发库；只读核对 `flyway_schema_history` 为版本 1，三张 Phase 1 表均存在。

结果：TASK-004 验收通过。未创建 Controller、接入 API 或 AI 表，未修改 BOSS Collector。
