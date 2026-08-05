# TASK-027：实现单条 Information Analysis

状态：DONE
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

- [x] 单条合法 Analysis 成功
- [x] 绑定正确 Snapshot
- [x] 绑定真实 Prompt Version
- [x] 相同成功逻辑身份默认不重复调用
- [x] Provider Invocation 保存
- [x] Provider Usage 保存为 Actual
- [x] Schema 失败时 Analysis FAILED 且已有 Usage 不丢
- [x] 用户不能查看其他账号结果
- [x] AI disabled 行为明确
- [x] 测试通过
- [x] CURRENT_STATUS 指向 TASK-028

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

- 新增 `POST /api/v1/ai/analyses` 与 `GET /api/v1/ai/analyses/{analysisId}`；Owner 只来自 Session，写请求继续使用 CSRF。
- POST 接收 Information、Prompt Profile 和可选 Snapshot；Snapshot 为空时解析当前版本，明确 Snapshot 时校验其属于指定 Information。
- 准备短事务锁定 Owner Profile，冻结 Active Prompt Version、当前 Definition Version、不可变 Snapshot、稳定三消息 Provider Request 和单条 Estimate。
- 使用数据库五元唯一键实现逻辑幂等：`SUCCEEDED` 直接复用，`FAILED` 仅显式重试并追加 Invocation，`RUNNING` 拒绝重复调用。
- Provider 配置预检发生在写入前；AI disabled 或配置不完整时不创建 Analysis/Invocation，也不访问网络。
- RUNNING Analysis 与 Invocation 提交后，Provider HTTP 在无数据库事务的编排 Service 中执行；成功、Provider 失败和输出失败分别使用新的完成短事务。
- Actual Token 只从 Provider Usage 写入 Invocation；Usage 缺失保持 NULL/UNAVAILABLE，Estimate 不回填 Actual。
- Provider 已成功但 JSON/Schema 校验失败时，Invocation 和 Usage 保留，Analysis 标记 FAILED，非法原始响应不入库。
- timeout 标记 Invocation TIMEOUT；其他 ambiguous 调用标记 UNKNOWN，均不自动重试。
- Analysis 查询同时过滤 `analysisId + userId`，跨账号与不存在统一返回 404；普通 GET 不触发 Provider。
- Snapshot 内部查询只读取标准化投影，不查询或返回 `raw_payload`。
- 未增加 Migration、Batch、Schedule、Preview、前端、推荐或通知。

## 9. 测试结果

- 使用 JDK 21 执行相关测试：
  - `.\mvnw.cmd "-Dtest=AnalysisTokenEstimatorTest,InformationAnalysisServiceTest,InformationAnalysisControllerTest,AnalysisDefinitionRegistryTest,AiProviderDomainTest,FakeAiProviderClientTest,OpenAiCompatibleChatClientTest" test`
  - 结果：通过；22 tests，0 failures，0 errors，0 skipped。
- 通过 SSH 隧道连接安全测试库执行 `.\mvnw.cmd "-Dtest=InformationAnalysisTransactionServiceIntegrationTest" test`
  - 结果：通过；1 test，0 failures，0 errors，0 skipped；真实验证逻辑幂等、FAILED 显式重试、Invocation attempt、Actual Usage 与 Owner 隔离。
- 使用相同真实 MySQL 环境执行 `.\mvnw.cmd test`
  - 结果：通过；127 tests，0 failures，0 errors，1 skipped，即 126 tests 实际执行通过。
  - 唯一跳过项为需要独立 `INFORMATION_HUB_EMPTY_TEST_DB_*` 空库环境的 `Phase3EmptySchemaMigrationIntegrationTest`，不属于 TASK-027 遗留。
- `.\mvnw.cmd -o -DskipTests compile`
  - 结果：通过；154 个主代码源文件使用 Java 21 编译成功。
- `.\mvnw.cmd -o -DskipTests package`
  - 结果：通过；生成可执行 Spring Boot JAR。
- TASK-027 单元与 MVC 测试使用 Mock/Fake Provider，不访问真实 AI，不需要 API Key。

## 10. 遗留问题

- TASK-027 真实 MySQL 事务集成测试及既有数据库回归已通过；Flyway 确认 `information_hub_test` 处于 V2 且无需迁移。
- TASK-028 负责 Candidate Resolver、Preview、HMAC Token 及候选/预算 Estimate；TASK-027 只提供可复用的单条 Estimate。
- TASK-029 负责 Worker 对崩溃遗留 RUNNING Invocation 的恢复；TASK-027 对 timeout/unknown 不自动重试。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
