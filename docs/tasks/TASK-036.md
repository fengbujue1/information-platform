# TASK-036：User Interaction、Feedback 与 Job Disposition

状态：DONE

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

## 8. 实施记录

完成时间：2026-08-09

实际实施：

- 新增 Session + CSRF 保护的 View、Feedback PUT 与 Job Disposition PUT API；
- 使用 MySQL `INSERT ... ON DUPLICATE KEY UPDATE` 原子维护 `(userId, informationId)` current aggregate，重复和并发首次 View 不丢计数；
- Feedback 与 JOB disposition 分别写入 Generic Core 和 JOB Extension，支持覆盖及恢复为 `NONE`；
- Job disposition 写入前校验目标 Information 为 JOB；
- 可选 Recommendation Item 归因校验 Run Owner 与同一 Information，不接受跨 Owner 或错配归因；
- 新增 Interaction / disposition 业务日志，不记录聊天或敏感内容；
- 未增加 Migration，V1～V4 均未修改；未触发 Profile、Analysis、Recommendation Run 或 Refresh；未实施 TASK-037。

验证：

- `java -version`：Java 21.0.11；
- `.\mvnw.cmd -version`：Maven Wrapper 3.9.16；
- `.\mvnw.cmd -DskipTests compile`：通过；
- `.\mvnw.cmd "-Dtest=RecommendationInteractionControllerTest,RecommendationInteractionServiceTest,RecommendationInteractionServiceIntegrationTest" test`：9 tests，0 failures，0 errors，0 skipped；真实 MySQL 覆盖 repeated view、feedback replace/clear、CONTACTED、CONTACTED_NOT_SUITABLE、disposition clear、独立状态与 8 路 concurrent upsert。
- `.\mvnw.cmd test`：208 tests，0 failures，0 errors，2 skipped；完整后端回归通过。
