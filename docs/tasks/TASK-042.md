# TASK-042：Recommendation Feed Query API

状态：TODO

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

实现轻量 Feed，基于最近成功 Run，同时应用当前 Interaction visibility 与状态展示。

## 2. 前置依赖

TASK-036、040；Contract `recommendation-feed-v1.md`。

## 3. 实施范围

- latest COMPLETED run；
- pagination；
- score/reasons；
- Job DTO reuse；
- feedbackState；
- jobDisposition；
- NOT_INTERESTED immediate hide；
- CONTACTED_NOT_SUITABLE immediate hide；
- CONTACTED visible；
- profileChangedSinceRun；
- Owner isolation。

## 4. 明确不做

- GET realtime scoring；
- worker trigger；
- UI。

## 5. 验收与测试

old feed while running/failed、switch after completed、pagination、owner、profile stale、CONTACTED visible、hard exclusion immediate hide、state reset re-show。

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
