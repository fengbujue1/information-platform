# TASK-037：Recommendation Candidate Resolver

状态：DONE

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

基于 Profile、当前 Snapshot、Phase 3 Analysis 和 Interaction hard exclusion 产生 JOB Candidate。

## 2. 前置依赖

TASK-034～036。

## 3. 实施范围

- JOB only；
- current usable Information；
- current Snapshot；
- frozen window；
- frozen Prompt Version；
- successful USER_RELEVANCE；
- latest compatible definition；
- NOT_INTERESTED exclusion；
- CONTACTED_NOT_SUITABLE exclusion；
- CONTACTED 保留；
- excludedKeywords；
- stable query；
- EXPLAIN。

## 4. 明确不做

- Scoring；
- Embedding；
- Provider；
- 新 source ID 的语义永久封禁。

## 5. 验收与测试

- old snapshot excluded；
- wrong prompt excluded；
- failed analysis excluded；
- NOT_INTERESTED excluded；
- CONTACTED_NOT_SUITABLE excluded；
- CONTACTED included；
- exclusion survives snapshot change；
- window boundary。

## 6. 文档同步

- Architecture/query notes if changed
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

- 新增只读 JOB Recommendation Candidate Resolver，不创建 Analysis、Recommendation Run 或 Recommendation Item；
- 候选固定使用当前可用 JOB、当前 Snapshot、左闭右开 frozen window、frozen Prompt Version，以及已注册且 System Prompt / Output Schema 语义兼容的成功 `JOB_USER_RELEVANCE` Analysis；
- 同一 Information 存在多个兼容成功 Analysis 时，按 Definition Version、Analysis ID 降序选择最新结果；
- 在数据库查询中实施 `NOT_INTERESTED`、`CONTACTED_NOT_SUITABLE` 和 Profile `excludedKeywords` hard exclusion，`CONTACTED` 保留；
- Interaction exclusion 按 `(userId, informationId)` 判断，Snapshot 更新后仍然有效；排除关键词仅对当前 Snapshot 的 title、content 与标准化 `job.companyName` 做字面匹配；
- 稳定查询顺序为 `first_seen_time DESC, information_id DESC, definition_version DESC, analysis_id DESC`；
- 使用真实 MySQL 对实际 MyBatis SQL 执行 `EXPLAIN FORMAT=JSON`，现有 V1～V4 索引满足当前查询，无需新增 Migration；
- 增加候选解析成功/空结果的聚合计数与耗时日志，不记录用户 ID、Profile 内容或 Analysis 内容；
- 未实施 Scoring、Ranking、Run、Trigger、Feed 或 TASK-038。

验证：

- Java 21.0.11 / Maven Wrapper 3.9.16 环境检查通过；
- TASK-037 定向测试覆盖 old snapshot、wrong prompt、failed analysis、两类 hard exclusion、`CONTACTED` 保留、跨 Snapshot exclusion、窗口边界、关键词排除、最新兼容 Definition、稳定顺序及真实 MySQL EXPLAIN；
- `./mvnw.cmd "-Dtest=JobRecommendationCandidateResolverTest,JobRecommendationCandidateQueryMapperSqlTest,JobRecommendationCandidateResolverIntegrationTest" test`：3 tests，0 failures，0 errors，0 skipped；
- `./mvnw.cmd test`：211 tests，0 failures，0 errors，2 skipped；完整后端回归通过。
