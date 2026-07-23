# TASK-004：创建 Phase 1 数据库结构

状态：TODO  
所属阶段：Phase 1  
优先级：P0

## 前置依赖

- TASK-002 完成。
- TASK-003 完成。

## 目标

使用 Flyway 创建：

- `information_item`
- `job_information`
- `information_snapshot`

## 本任务范围

- 配置 MySQL、Flyway、MyBatis-Plus。
- 创建 V1 迁移。
- 创建唯一索引、查询索引和外键。
- 创建 Entity 和 Mapper。
- 使用真实 MySQL 兼容测试验证 JSON 字段和迁移。
- 验证快照唯一约束。

## 关键规则

- 时间统一存 UTC。
- 保存 `collector_id`、`collector_version` 和可选 `collection_context`。
- 不使用框架自动建表。
- 已执行迁移不可修改。
- 快照是不可变记录。
- 快照删除策略在实施前确认，推荐 `ON DELETE RESTRICT`。

## 不在范围

- 不实现 Controller。
- 不实现接入 Service。
- 不创建 AI、用户、推荐或通知表。

## 验收标准

- [ ] Flyway 成功执行
- [ ] 三张表创建成功
- [ ] 幂等唯一索引有效
- [ ] JSON 可写入读取
- [ ] 外键行为符合设计
- [ ] 相同 informationId + contentHash 不能重复插入快照
- [ ] Maven 测试通过
