# TASK-040：Recommendation Run、Worker 与 Manual Refresh

状态：DONE

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

## 8. 实施记录

完成时间：2026-08-09

实际实施：

- 实现 `POST /api/v1/recommendations/{informationType}/refresh`，以 Session Owner 与 CSRF 保护 Manual Refresh，成功返回 `202 PENDING`；
- 在创建短事务中锁定 Owner + Information Type 的 Profile Core，串行化并发 Manual 请求，并对已有 `PENDING/RUNNING` Manual Run 返回 `409 RECOMMENDATION_RUN_IN_PROGRESS`；
- Run 创建时冻结 Generic Profile Core、JOB Profile Extension、Prompt Active Version、算法 Key/Version 与 UTC 左闭右开候选窗口；
- 实现 Owner + Information Type 隔离的 Run list/detail API，响应不暴露 userId 与完整 Profile Snapshot；
- 实现 `PENDING -> RUNNING -> COMPLETED/NOOP/FAILED` 生命周期、`FOR UPDATE SKIP LOCKED` 并发 claim 与 stale RUNNING recovery；
- Candidate、Scoring、Ranking 在数据库事务外执行，不调用 Preview、Analysis Batch 或 AI Provider；
- 最终短事务一次性插入全部不可变 Recommendation Item 后才发布 Run 终态；任一 Item 写入失败时整体回滚，历史 COMPLETED Run 不被覆盖；
- Worker 只消费既有 PENDING Run，未实现 Auto Analysis Batch hook、Feed API 或独立 Recommendation Cron；
- 增加 Manual 请求、Run 创建/开始/完成/失败、Candidate 解析、Scoring 与 stale recovery 聚合日志，包含业务 ID、计数与 duration，异常日志经过脱敏；
- Recommendation Worker 默认关闭，可通过服务端环境变量显式启用并配置轮询与 stale 阈值；
- 未修改数据库结构或 Flyway Migration，继续使用 TASK-034/TASK-034A 已落地的 V3/V4 表结构。

验证：

- `./mvnw.cmd "-Dtest=RecommendationRunControllerTest,RecommendationRunCreationTransactionServiceTest,RecommendationRunWorkerTransactionServiceTest,RecommendationRunWorkerTest" test`：13 tests，0 failures，0 errors，0 skipped；
- `./mvnw.cmd test`：244 tests，0 failures，0 errors，2 skipped；完整后端回归与真实 MySQL/Flyway V4 校验通过。
