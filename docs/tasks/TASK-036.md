# TASK-036：User Interaction、Feedback 与 Job Disposition

状态：TODO

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

实现 VIEWED、INTERESTED、NOT_INTERESTED、CONTACTED、CONTACTED_NOT_SUITABLE 的 current user × information 状态。

## 2. 前置依赖

TASK-034；Contract `recommendation-interaction-v1.md`。

## 3. 实施范围

- View API；
- Feedback PUT；
- Job Disposition PUT；
- `(userId, informationId)` upsert；
- viewCount / lastViewedAt；
- feedback timestamps；
- disposition timestamps；
- optional recommendationItem attribution；
- Owner isolation；
- interaction/disposition logs。

## 4. 明确不做

- 自动 BOSS 聊天记录同步；
- 自动修改 Recommendation Profile；
- 自动 Recommendation Refresh；
- 完整行为事件流水。

## 5. 验收与测试

- repeated view；
- feedback replace / clear；
- CONTACTED；
- CONTACTED_NOT_SUITABLE；
- disposition clear；
- feedback 与 disposition 独立；
- attribution owner；
- concurrent upsert。

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
