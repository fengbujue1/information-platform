# TASK-041：Analysis Batch Completion → Recommendation Auto Trigger

状态：DONE

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

将 Phase 3 Analysis Batch 终态与 Phase 4 Recommendation Run 解耦连接，并保证幂等。

## 2. 前置依赖

TASK-040；ADR-015。

## 3. 实施范围

- AnalysisBatchTerminalEvent；
- AFTER_COMMIT；
- COMPLETED/PARTIAL_FAILED；
- Profile exists；
- same Owner；
- same bound Prompt Profile；
- freeze source Batch Prompt Version；
- unique sourceAnalysisBatchId；
- FAILED/NOOP skip；
- MANUAL/SCHEDULED Batch；
- trigger logs。

## 4. 明确不做

- Recommendation Cron；
- Single Analysis trigger；
- Profile update trigger；
- Collector trigger；
- Kafka。

## 5. 验收与测试

COMPLETED、PARTIAL_FAILED、FAILED、NOOP、mismatch、no profile、duplicate event、both batch trigger types。

## 6. 文档同步

- ARCHITECTURE
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

- 新增进程内 `AnalysisBatchTerminalEvent`，由 Manual/Scheduled Batch 创建事务和 Batch Worker 终态收敛事务统一发布；
- 事件覆盖 `COMPLETED`、`PARTIAL_FAILED`、`FAILED` 与 `NOOP`，PENDING/RUNNING 不发布；
- 新增严格 `AFTER_COMMIT` 的 Recommendation Auto Trigger Listener，使用独立 `REQUIRES_NEW` 短事务创建 Run，Recommendation 失败不会回滚已提交的 Analysis Batch；
- 仅允许来源 Batch 状态为 `COMPLETED/PARTIAL_FAILED`、触发类型为 `MANUAL/SCHEDULED`、Information Type 为 JOB；`FAILED/NOOP` 使用稳定原因跳过；
- 通过 Batch Owner + Information Type 查询并锁定当前 Profile，要求 Profile 存在且绑定同一 Prompt Profile；
- Auto Run 冻结当前 Generic Profile Core + JOB Extension，并严格使用来源 Batch 的 Prompt Profile/Prompt Version；
- Auto Run 使用 `triggerType = ANALYSIS_BATCH_COMPLETED` 和 `sourceAnalysisBatchId`，复用 V3/V4 已有唯一约束保证同一 Batch 最多一个 Run；
- 重复事件先读取既有 Run，数据库唯一键作为并发最终防线；未新增 Migration；
- 增加 trigger evaluating、created、skipped、failed 与 duration 聚合日志，不记录 Prompt、Token、Payload 或其它敏感内容；
- 未实现 Recommendation Cron、Single Analysis/Profile/Collector Trigger、Kafka 或 TASK-042 Feed API。

验证：

- Java 21.0.11 / Maven Wrapper 3.9.16 环境检查通过；
- `./mvnw.cmd "-Dtest=AnalysisBatchTerminalEventPublisherTest,AnalysisBatchTransactionServiceTest,AnalysisBatchWorkerTransactionServiceTest,RecommendationRunCreationTransactionServiceTest,RecommendationAutoTriggerListenerTest,RecommendationAutoTriggerIntegrationTest" test`：20 tests，0 failures，0 errors，0 skipped；
- `./mvnw.cmd test`：254 tests，0 failures，0 errors，2 skipped；完整后端回归与真实 MySQL/Flyway V4 校验通过。
