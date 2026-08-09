# TASK-039：Ranking、Deduplication、Diversity 与 Top N

状态：DONE

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

## 8. 实施记录

完成时间：2026-08-09

实际实施：

- 新增纯计算 `JobRecommendationRanker`，stable comparator 固定为 `finalScore DESC, freshnessScore DESC, informationId DESC`；
- 对 companyName、title、cityName 执行 NFKC、大小写与空白规范化，再用带长度前缀的 canonical input 计算 64 位小写 SHA-256 `duplicateGroupKey`；
- 稳定排序后按 duplicate group 去重，同组保留最高优先级 Candidate；
- 前 20 个结果实施同 company 最多 3、同 normalized title 最多 5 的基础多样性限制；
- diversity pass 不足 topN 时，按去重后的稳定原始顺序执行 fill pass；
- 最终结果限制为 topN，并生成从 1 开始的连续 rankNo，同时保留 TASK-038 score breakdown 与 reasons；
- 增加 scored、deduplicated、result、topN 与 duration 的聚合日志，不记录 Candidate 内容；
- 未实施 Embedding、语义聚类、Reranker、ML，也未创建 Recommendation Run / Item 或实施 TASK-040。

验证：

- Java 21.0.11 / Maven Wrapper 3.9.16 环境检查通过；
- `./mvnw.cmd "-Dtest=JobRecommendationRankerTest" test`：8 tests，0 failures，0 errors，0 skipped；
- `./mvnw.cmd test`：231 tests，0 failures，0 errors，2 skipped；完整后端回归通过。
