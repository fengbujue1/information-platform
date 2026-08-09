# TASK-042：Recommendation Feed Query API

状态：DONE

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

## 8. 实施记录

完成日期：2026-08-09

已实施：

- 新增 Session Owner 保护的 `GET /api/v1/recommendations/{informationType}/feed`，默认 `page=1`、`pageSize=20`，单页最大 100；Phase 4 只接受 JOB；
- Feed 只选择同 Owner、同 Information Type 最近 `COMPLETED` Run；较新的 PENDING、RUNNING、FAILED 不覆盖旧成功 Feed，新 Run 仅在 COMPLETED 后切换；
- 在同一个只读事务快照内完成 Run 选择、当前 Profile hash 比较、可见 Item 统计与分页；没有成功 Run 时返回 `run=null` 和空 Items，不触发实时计算；
- SQL 查询基于当前 `user_information_interaction` 与 `user_job_disposition` 应用 visibility：`NOT_INTERESTED` 和 `CONTACTED_NOT_SUITABLE` 立即隐藏，`CONTACTED` 保持可见；恢复为 NONE 后仍属于当前 Run 的 Item 可重新显示；
- 返回预计算 final score、score breakdown、reasons、冻结 Snapshot/Rank、current Feedback/Disposition/View 状态，以及复用现有 Job Query DTO 语义的轻量 JOB 展示字段；
- 当前 Profile contentHash 与 Run 冻结 hash 不同时返回 `profileChangedSinceRun=true`，只提示，不自动 Refresh；
- 增加 Feed 完成 INFO（Run、分页、总数、`durationMs`）和脱敏持久化异常 ERROR，不记录敏感内容；
- 未修改数据库或 Flyway，未实现 GET 实时评分、Worker Trigger、TASK-043 UI 或后续 TASK。

验证：

- `java -version`：Java 21.0.11；
- `.\mvnw.cmd -version`：Maven Wrapper 3.9.16；
- `.\mvnw.cmd "-Dtest=RecommendationFeedServiceTest,RecommendationFeedControllerTest,RecommendationFeedServiceIntegrationTest" test`：8 tests，0 failures，0 errors，0 skipped；真实 MySQL 覆盖旧 Feed 保留、完成切换、分页、Owner、Profile stale、CONTACTED 可见、hard exclusion 即时隐藏和状态恢复；
- `.\mvnw.cmd test`：262 tests，0 failures，0 errors，2 skipped，BUILD SUCCESS；
- Flyway 在真实 MySQL 上验证 V1～V4，数据库保持 V4，无新 Migration。
