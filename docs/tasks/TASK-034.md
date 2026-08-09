# TASK-034：Phase 4 Recommendation Data Model 与 Flyway

状态：TODO

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
