# TASK-024：实现 Phase 3 数据模型与 Flyway

状态：DONE
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

- [x] Flyway migration 可在空测试库执行
- [x] 旧 migration 未修改
- [x] 表结构与 Accepted `DATABASE_DESIGN_PHASE3_DRAFT.md` 一致
- [x] FK / index / unique 与 Accepted 设计和合同一致
- [x] Snapshot 不被 AI 级联删除
- [x] Prompt Version 历史受保护
- [x] Model Invocation 能表达 Usage unavailable
- [x] Schedule 默认 disabled / 02:00
- [x] MyBatis-Plus Mapper 测试通过
- [x] 空测试库全量 migrate 通过
- [x] 既有测试库 upgrade 通过
- [x] `docs/DATABASE_DESIGN.md` 已合并真实 Phase 3 表结构
- [x] Phase 3 Database Design 文档与实际 migration 一致
- [x] CURRENT_STATUS 指向 TASK-021

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

- 读取真实 migration 后确认下一版本为 V2，未修改 V1。
- 新增 `V2__create_phase3_ai_processing_tables.sql`：
  - 增加 FIRST_INGESTED 查询索引；
  - 严格创建 Accepted 的 8 张 Phase 3 表；
  - 实现冻结的字段类型、NULL/default、PK、UNIQUE、核心索引、CHECK、FK 和 RESTRICT 规则。
- 新增 `identity` 持久层 `UserAccountPo` / `UserAccountMapper`。
- 新增 `analysis` 持久层 7 组 PO/Mapper。
- 所有新增数据实体字段均添加中文业务注释。
- 新增 Phase 3 空库、既有库升级、结构约束和 Mapper 集成测试。
- 更新 Phase 1 migration 测试的当前版本断言为 V2。
- 新增数据库测试目标安全校验：
  - 普通数据库集成测试只接受名称包含 `test` 且不包含 `dev` 的数据库；
  - 空库测试额外要求数据库名包含 `empty`；
  - 防止环境变量错误导致测试再次迁移开发库。
- 所有新增文件均已通过 `git add` 纳入追踪。

## 9. 测试结果

以下测试均使用 Java 21、Maven 3.9.16、MySQL 8.4.10，通过本地 SSH 隧道执行。

普通测试：

```text
mvn test
```

结果：

```text
Tests run: 53, Failures: 0, Errors: 0, Skipped: 18
BUILD SUCCESS
```

18 个 Skipped 是显式移除数据库环境变量后的条件数据库测试；普通单元、Controller、Mapper SQL 和数据库目标安全测试通过。

既有测试库 V1 → V2 升级与回归：

```text
mvn -Dtest=Phase3SchemaMigrationIntegrationTest,Phase3MapperIntegrationTest,\
Phase1SchemaMigrationIntegrationTest,InformationIngestionIntegrationTest,\
JobQueryIntegrationTest test
```

结果：

```text
information_hub_test: V1 -> V2
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

专用空库全量 migration：

```text
mvn -Dtest=Phase3EmptySchemaMigrationIntegrationTest test
```

结果：

```text
information_hub_test_task024_empty: << Empty Schema >> -> V1 -> V2
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

空库迁移后的结构、Mapper 和 Phase 1 回归：

```text
mvn -Dtest=Phase3SchemaMigrationIntegrationTest,Phase3MapperIntegrationTest,\
Phase1SchemaMigrationIntegrationTest test
```

结果：

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

临时库完成验证后已撤销测试账号权限、删除数据库，并通过 `INFORMATION_SCHEMA.SCHEMATA` 确认不存在。

修正测试库目标并恢复 SSH 隧道后的最终完整套件：

```text
mvn test
Tests run: 53, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

唯一 Skipped 为已经单独在专用临时空库通过的空库 migration 测试。

## 10. 遗留问题

- 当前 Flyway 在 MySQL 8.4 上提示“最新已测试版本为 MySQL 8.1”的升级建议；真实 migration 和测试均成功，后续依赖升级任务再评估 Flyway 版本。
- MySQL 对 `TINYINT(1)` 给出 display width 弃用警告；当前类型来自 Accepted 设计且 Boolean 映射测试通过，不在 TASK-024 临场改表。
- 测试配置曾把 `INFORMATION_HUB_TEST_DB_URL` 指向 `information_hub_dev`，开发库因此也从 V1 升级到 V2。未执行 clean、降级或数据删除；已通过数据库名称安全校验防止再次发生。
- `C:\Users\Admin\.codex\.env` 应将 `INFORMATION_HUB_TEST_DB_*` 固定指向 `information_hub_test`；专用空库只在需要全量 migration 验证时临时创建。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
