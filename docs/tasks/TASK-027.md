# TASK-027：实现单条 Information Analysis

状态：TODO  
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

实现一个登录用户针对一个确定 Snapshot 使用指定 Prompt Profile 当前版本执行一次可追溯 USER_RELEVANCE 分析的完整后端链路。

## 2. 前置依赖

- TASK-026 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- 单条分析 API / Application Service。
- Resolve Current User。
- Resolve Prompt Profile Active Version。
- Resolve current/explicit Snapshot。
- 逻辑幂等检查。
- Estimate。
- 创建 Information Analysis。
- 调用 Prompt Assembly + Provider。
- 创建 Model Invocation。
- 保存 Provider Usage。
- Schema Validation。
- SUCCEEDED / FAILED 状态。
- Analysis 查询 API。
- 用户 Owner 隔离。
- 对 ambiguous timeout 使用已冻结策略。

## 4. 不在本任务范围

- 不实现批量 Preview。
- 不实现 Batch Worker。
- 不实现 Schedule。
- 不实现推荐。
- 不开放普通 GET 自动触发 AI。
- 不强制 reanalysis 功能。

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

- [ ] 单条合法 Analysis 成功
- [ ] 绑定正确 Snapshot
- [ ] 绑定真实 Prompt Version
- [ ] 相同成功逻辑身份默认不重复调用
- [ ] Provider Invocation 保存
- [ ] Provider Usage 保存为 Actual
- [ ] Schema 失败时 Analysis FAILED 且已有 Usage 不丢
- [ ] 用户不能查看其他账号结果
- [ ] AI disabled 行为明确
- [ ] 测试通过
- [ ] CURRENT_STATUS 指向 TASK-028

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
