# TASK-039：Ranking、Deduplication、Diversity 与 Top N

状态：TODO

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

将已评分 Candidate 形成稳定、去重、具基础多样性的 Top N。

## 2. 前置依赖

TASK-038。

## 3. 实施范围

- stable comparator；
- duplicateGroupKey；
- same duplicate group highest score；
- company/title caps；
- fill pass；
- topN；
- rankNo。

## 4. 明确不做

- Embedding similarity；
- semantic clustering；
- reranker；
- ML。

## 5. 验收与测试

equal score tie、duplicate、company cap、title cap、fill、topN、deterministic repeatability。

## 6. 文档同步

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
