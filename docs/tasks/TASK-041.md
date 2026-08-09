# TASK-041：Analysis Batch Completion → Recommendation Auto Trigger

状态：TODO

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
