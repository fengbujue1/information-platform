# TASK-004：创建 Phase 1 数据库结构

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 前置依赖

TASK-002、TASK-003 完成。

## 目标

使用 Flyway 创建 `information_item` 和 `job_information`。

## 本任务范围

- 配置 MySQL、Flyway、MyBatis-Plus。
- 创建 V1 迁移。
- 创建唯一索引、查询索引和外键。
- 创建 Entity 和 Mapper。
- 使用 Testcontainers 或等效真实 MySQL 测试迁移和 JSON 字段。

## 关键规则

- 时间统一存 UTC。
- 保存 collector_id 和 collector_version。
- 不使用框架自动建表。
- 已执行迁移不可修改。

## 验收标准

- [ ] Flyway 成功
- [ ] 幂等唯一索引有效
- [ ] JSON 可写入读取
- [ ] 外键和级联符合设计
- [ ] 测试通过
