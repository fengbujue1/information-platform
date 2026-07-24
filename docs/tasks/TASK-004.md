# TASK-004：创建 Phase 1 数据库结构

状态：TODO  
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
