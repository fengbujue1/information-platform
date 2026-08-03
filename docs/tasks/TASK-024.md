# TASK-024：实现 Phase 3 数据模型与 Flyway

状态：TODO  
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

严格按照 TASK-020 已 Accepted 的 Phase 3 领域模型和物理数据库设计实现新的 MySQL/Flyway 结构、索引、FK 和 MyBatis-Plus 持久化基础。本任务是“数据库设计实现”，不是重新设计数据库。

## 2. 前置依赖

- TASK-020 已完成，Phase 3 Data Model 与 Database Design 已 Accepted。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 必须阅读 Accepted `docs/PHASE3_DATA_MODEL_DRAFT.md`。
- 必须阅读 Accepted `docs/DATABASE_DESIGN_PHASE3_DRAFT.md`。
- 必须对照当前 `docs/DATABASE_DESIGN.md` 和全部已有 Flyway migration。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- 根据真实最新 Flyway 版本号新增下一条 migration；不得假定文件一定叫 V2。
- 严格创建 Accepted Database Design 中的表。
- 严格实现 Accepted 的唯一约束和索引。
- Snapshot FK。
- Prompt Version 保护。
- 实现已冻结的 Schedule trigger 幂等约束。
- UTC 时间规则。
- PO/Mapper。
- 数据库条件测试。
- 执行空测试库全量 migration 和已有测试库 upgrade 验证。
- 更新 `docs/DATABASE_DESIGN.md`，把真正落地的 Phase 3 表结构合并为数据库事实。
- 更新 `docs/DATABASE_DESIGN_PHASE3_DRAFT.md` 状态/实施记录，使文档与真实 SQL 一致。

## 4. 不在本任务范围

- 不实现 Provider。
- 不执行真实 AI。
- 不引入新数据库产品。
- 不修改已执行的旧 Flyway migration。
- 不实现前端页面。
- 不在实施过程中擅自改变 Accepted 的表数量、字段语义、FK、UNIQUE、核心索引、Token 事实来源或 Schedule 幂等策略。
- 如果 Accepted 设计与真实数据库/代码冲突，必须停止实现、汇报并回到设计文档，经用户确认后继续。

## 5. 实施原则

- 只完成当前 TASK。
- 不覆盖来源事实。
- 不把 rawPayload 默认发送给模型。
- 不把秘密写入 Git 或前端。
- 不提前引入 Kafka、Redis、Elasticsearch、向量数据库、RAG 或微服务。
- 不提前实现推荐和通知。
- Codex 修改前必须先汇报计划并等待确认。
- Codex 不提交 Git，由用户检查后提交。

## 6. 验收标准

- [ ] Flyway migration 可在空测试库执行
- [ ] 旧 migration 未修改
- [ ] 表结构与 Accepted `DATABASE_DESIGN_PHASE3_DRAFT.md` 一致
- [ ] FK / index / unique 与 Accepted 设计和合同一致
- [ ] Snapshot 不被 AI 级联删除
- [ ] Prompt Version 历史受保护
- [ ] Model Invocation 能表达 Usage unavailable
- [ ] Schedule 默认 disabled / 02:00
- [ ] MyBatis-Plus Mapper 测试通过
- [ ] 空测试库全量 migrate 通过
- [ ] 既有测试库 upgrade 通过
- [ ] `docs/DATABASE_DESIGN.md` 已合并真实 Phase 3 表结构
- [ ] Phase 3 Database Design 文档与实际 migration 一致
- [ ] CURRENT_STATUS 指向 TASK-021

## 7. 实施前必须汇报

- 当前真实代码与文档基线；
- 前置依赖是否满足；
- 计划修改文件；
- 数据流 / 事务 / 安全边界；
- 测试计划；
- 风险；
- 与 Draft 设计不一致的地方；
- 明确不实施的内容。

## 8. 实施记录

待填写。

## 9. 测试结果

必须填写实际执行命令和结果，禁止编造。

## 10. 遗留问题

待填写。

## 11. 完成确认

- [ ] 当前 TASK 文档已更新
- [ ] `docs/CURRENT_STATUS.md` 已更新
- [ ] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
