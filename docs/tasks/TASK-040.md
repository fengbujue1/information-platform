# TASK-040：Recommendation Run、Worker 与 Manual Refresh

状态：TODO

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

实现预计算 Recommendation Run 执行引擎、Worker、Manual Refresh 与 Run 状态 API。

## 2. 前置依赖

TASK-034～039；Contract `recommendation-refresh-v1.md`。

## 3. 实施范围

- Run lifecycle；
- PENDING claim；
- Worker；
- stale RUNNING recovery；
- Manual 202；
- Run list/detail；
- short final transaction；
- Item immutable；
- old completed feed preserved；
- logs。

## 4. 明确不做

- Auto Analysis Batch hook；
- Feed API；
- Provider；
- independent recommendation cron。

## 5. 验收与测试

manual run、concurrency、recovery、failure history、atomic visibility、no Provider call、duration logs。

## 6. 文档同步

- Contract implementation note
- CURRENT_STATUS

## 7. 完成前统一检查

- 当前 TASK 测试通过；
- `git diff --check`；
- 未超 Scope；
- 后端改动按 `docs/LOGGING_CONVENTIONS.md` 检查；
- 更新当前 TASK 实施记录；
- 更新 `docs/CURRENT_STATUS.md`；
- 如数据库/架构事实改变，同步事实文档；
- 汇报实际命令与结果。
