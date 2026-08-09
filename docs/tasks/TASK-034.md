# TASK-034：Phase 4 Recommendation Data Model 与 Flyway

状态：DONE

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

实施四张 Phase 4 核心表、PO/Mapper、索引和数据库集成测试。

## 2. 前置依赖

TASK-033 完成；实施前检查真实最新 Flyway version。

## 3. 实施范围

- user_recommendation_profile；
- user_information_interaction，含 feedback_state / job_disposition；
- recommendation_run；
- recommendation_item；
- Flyway；
- PO / Mapper；
- migration / integration tests；
- EXPLAIN 后确定必要组合索引。

## 4. 明确不做

- Profile API；
- Recommendation Algorithm；
- Web；
- 修改既有 migration。

## 5. 验收与测试

覆盖空库、升级、FK/UNIQUE、interaction unique、jobDisposition 合法值应用校验、auto source batch uniqueness、item rank uniqueness、Mapper CRUD。

## 6. 文档同步

- `docs/DATABASE_DESIGN.md`
- `docs/CURRENT_STATUS.md`

## 7. 完成前统一检查

- 当前 TASK 测试通过；
- `git diff --check`；
- 未超 Scope；
- 后端改动按 `docs/LOGGING_CONVENTIONS.md` 检查；
- 更新当前 TASK 实施记录；
- 更新 `docs/CURRENT_STATUS.md`；
- 如数据库/架构事实改变，同步事实文档；
- 汇报实际命令与结果。

## 8. 实施记录（2026-08-09）

- 新增 `V3__create_phase4_recommendation_tables.sql`，在既有 V1/V2 之后创建四张 Phase 4 核心表；
- 全部历史与 Owner 外键使用 `ON DELETE/UPDATE RESTRICT`，未修改 V1/V2；
- `last_recommendation_item_id` 选择 nullable FK：先创建 Interaction，再在 Item 创建后通过 `ALTER TABLE` 补充 RESTRICT FK；
- 实施 Profile Owner 唯一、Interaction `(user_id, information_id)` 唯一、Auto Run 来源 Batch 唯一、Item Information/Rank 唯一；
- 新增四张表的 MyBatis-Plus PO/Mapper；JSON 字段映射为规范化 JSON 字符串，分数字段使用 `BigDecimal`；
- 新增 `FeedbackState` 与 `JobDisposition` 应用校验；`CONTACTED` 不属于 hard exclusion，`CONTACTED_NOT_SUITABLE` 属于 hard exclusion；
- 基于真实合法数据图执行 EXPLAIN，确认 Profile Owner、Interaction identity、Run Owner/Status 与 Item Run/Rank 查询具备候选组合索引；
- 未实现 Profile API、Interaction API、Recommendation Algorithm、Worker、Feed 或 Web。

验证结果：

- Java 21、Maven Wrapper 3.9.16 环境检查通过；
- 状态合法值测试 2 项通过；
- 专用升级测试库从 V2 成功迁移至 V3；升级、Schema、FK、UNIQUE、默认值、EXPLAIN、Mapper CRUD 相关 6 项通过；最终定向测试合计 8 项通过；
- 空库迁移测试已更新为 V1 → V3，但当前未配置 `INFORMATION_HUB_EMPTY_TEST_DB_URL`，因此该项按条件跳过；
- 未执行破坏性数据库清理，也未修改既有 migration。

后续说明：TASK-034 的 V3 是不可修改的历史事实；TASK-034A 已通过新增 V4 将其通用化为 Recommendation Core + JOB Domain Extension。
