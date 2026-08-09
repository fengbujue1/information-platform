# TASK-037：Recommendation Candidate Resolver

状态：TODO

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
