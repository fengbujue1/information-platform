# TASK-038：Recommendation Scoring 与 Explainability

状态：DONE

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

## 8. 实施记录

完成时间：2026-08-09

实际实施：

- 新增纯计算 `JobRecommendationScorer`，固定 Algorithm Key/Version 为 `JOB_RECOMMENDATION / 1`，不调用 AI Provider、不访问数据库；
- 按 `70% AI relevance + 20% JOB profile match + 10% freshness` 输出 0..100、三位小数的 final score 与 score breakdown；
- JOB Profile match 覆盖 target role、preferred skill、city、remote type、salary minimum，只有已配置维度进入等权分母；未配置任何维度时使用中性 100 分；
- freshness 按 `firstSeenTime` 在 frozen window 内线性归一化，窗口边界和窗口外输入稳定钳制到 0 / 100；
- 输出固定顺序的 AI、Profile 命中与 freshness reasons，并对候选结构化 JOB 字段执行类型校验；
- 批量评分保持 Candidate 输入顺序和重复调用结果一致，仅记录 algorithm、candidate count 与 duration 的聚合日志；
- 未实施 TASK-039 的 Ranking、Deduplication、Diversity、Top N，也未创建 Recommendation Run / Item 或修改数据库。

验证：

- Java 21.0.11 / Maven Wrapper 3.9.16 环境检查通过；
- `./mvnw.cmd "-Dtest=JobRecommendationScorerTest" test`：12 tests，0 failures，0 errors，0 skipped；
- `./mvnw.cmd test`：223 tests，0 failures，0 errors，2 skipped；完整后端回归通过。
