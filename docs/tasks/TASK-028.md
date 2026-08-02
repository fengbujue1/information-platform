# TASK-028：实现 Candidate Resolver、Preview 与 Token Estimate

状态：TODO  
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

实现 JOB 最近 N 天候选解析和无 AI 调用的 Preview，让用户在分析前看到候选规模、已分析数量、待分析数量和预计 Token。

## 2. 前置依赖

- TASK-027 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- 定义通用 CandidateResolver 接口。
- 只实现 JobCandidateResolver。
- 使用 TASK-020 已冻结的 FIRST_INGESTED 真实字段。
- 固定 windowStart/windowEnd。
- 当前 Snapshot 解析。
- 已分析判断。
- stable ordering。
- platform maxWindowDays。
- maxCandidates。
- TokenEstimator。
- estimated input/output/total。
- estimateMethod。
- Preview API。
- Preview 防止候选集合漂移的确认机制设计。
- 单元/数据库测试。

## 4. 不在本任务范围

- 不创建 Batch。
- 不调用真实 AI Provider。
- 不实现 NEWS/HOUSE/POLICY Resolver。
- 不做向量搜索。
- 不做全文检索。
- 不做推荐排序。

## 5. 实施原则

- 只完成当前 TASK。
- 不覆盖来源事实。
- 不把 rawPayload 默认发送给模型。
- 不把秘密写入 Git 或前端。
- 不提前引入 Kafka、Redis、Elasticsearch、向量数据库、RAG 或微服务。
- 不提前实现推荐和通知。
- Codex 修改前必须先汇报计划并等待确认。
- Codex 不提交 Git，由用户检查后提交。

## 6. 验收标准

- [ ] 输入 N 天得到固定绝对窗口
- [ ] 窗口语义与首次入库一致
- [ ] total/eligible/alreadyAnalyzed/pending 正确
- [ ] stable ordering 有测试
- [ ] 超过 maxWindowDays 被拒绝
- [ ] maxCandidates 影响可见
- [ ] Estimated Token 有明确 method
- [ ] Preview 不产生 Model Invocation
- [ ] 结果可供 Manual Confirm 安全复用
- [ ] CURRENT_STATUS 指向 TASK-029

## 7. 实施前必须汇报

- 当前真实代码与文档基线；
- 前置依赖是否满足；
- 计划修改文件；
- 数据流 / 事务 / 安全边界；
- 测试计划；
- 风险；
- 与 Draft 设计不一致的地方；
- 明确不实施的内容。

## 8. 实施记录

待填写。

## 9. 测试结果

必须填写实际执行命令和结果，禁止编造。

## 10. 遗留问题

待填写。

## 11. 完成确认

- [ ] 当前 TASK 文档已更新
- [ ] `docs/CURRENT_STATUS.md` 已更新
- [ ] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
