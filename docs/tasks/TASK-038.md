# TASK-038：Recommendation Scoring 与 Explainability

状态：TODO

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

实现 `JOB_RECOMMENDATION_V1` 确定性评分，不调用 AI。

## 2. 前置依赖

TASK-037；ADR-014。

## 3. 实施范围

- AI relevance 70%；
- Profile match 20%；
- Freshness 10%；
- configured-dimension normalization；
- 0..100；
- scoreBreakdown；
- reasons；
- algorithm key/version。

## 4. 明确不做

- ML；
- LLM；
- Ranking/Dedup/Diversity；
- dynamic weight admin。

## 5. 验收与测试

score bounds、各 profile dimension、freshness boundaries、repeatability、无 Provider invocation。

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
