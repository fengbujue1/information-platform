# TASK-025：实现 AI Provider、Usage Adapter 与 Fake Provider

状态：DONE
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

建立与具体模型供应商解耦的 `AiProviderClient`，统一 Provider Usage，并提供 CI 可用的 Fake Provider。

## 2. 前置依赖

- TASK-023 已完成。
- TASK-024 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- 定义 AiProviderClient。
- 定义统一 AiProviderRequest / Result。
- Provider metadata。
- Usage：input/output/total/cached/reasoning。
- `usageStatus`。
- request id、latency、finish/error metadata。
- Fake Provider。
- 第一家 OpenAI-compatible Provider 实现。
- 服务端配置：enabled/baseUrl/apiKey/model/timeout/maxOutputTokens。
- 默认 AI disabled。
- API Key 日志脱敏。
- 错误分类和安全 retry 基础。
- 单元测试不访问公网。

## 4. 不在本任务范围

- 不实现业务 Prompt Assembly。
- 不实现 Batch。
- 不实现 Schedule。
- 不把 API Key 放前端。
- 不让 CI 依赖真实 Provider。
- 不引入 LangChain4j/Spring AI；使用 Spring `RestClient` 和现有 Jackson。

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

- [x] Fake Provider 可稳定返回结构化响应和 Usage
- [x] OpenAI-compatible Adapter 可解析 Usage
- [x] Provider 无 Usage 时字段 NULL/UNAVAILABLE
- [x] API Key 不进入日志
- [x] AI disabled 时不会发网络请求
- [x] timeout/429/5xx 有明确分类
- [x] ambiguous timeout 不无脑重试
- [x] CI 不需要真实 Key
- [x] 测试通过
- [x] CURRENT_STATUS 指向 TASK-026

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

- 新增通用 `AiProviderClient`，请求只接收上层已组装的 Chat 消息与单次最大输出 Token，并在真实调用前提供 Provider/Model Invocation 元数据，不绑定 JOB、Prompt Profile 或 Analysis 持久化。
- 新增统一 `AiProviderResult`、`AiProviderUsage`、Usage Status、稳定错误分类和安全重试处置。
- Actual Usage 仅接受 Provider 报告值，不补算 input/output/total/cached/reasoning Token；Usage 缺失时状态为 `UNAVAILABLE` 且所有 Token 为 NULL。
- 新增确定性、无网络、可注入预设结构化文本和 Usage 的 `FakeAiProviderClient`。
- 新增基于 Spring `RestClient` 与现有 Jackson 的 `OpenAiCompatibleChatClient`，使用 Chat Completions `/chat/completions`。
- Adapter 解析 Provider/model/request id/finish reason/latency 和 OpenAI-compatible Usage，包括 cached/reasoning 可选明细。
- Provider 默认 disabled；真实配置只从服务端环境变量读取，启用前校验 baseUrl/apiKey/model/maxOutputTokens。
- API Key 只进入 Authorization Header；配置诊断不输出 API Key 或未经校验的 Base URL 原文，稳定异常和测试输出均脱敏，无效响应异常不保留可能回显原始响应的 Jackson cause。
- timeout 与已执行但响应无效的场景标记 `AMBIGUOUS_DO_NOT_AUTO_RETRY`；429 标记可退避重试；5xx 保守视为结果不确定。Client 内部不实现自动重试。
- 本任务没有数据库访问、事务、Invocation 持久化、Prompt Assembly、Analysis、Batch、Schedule、API 或前端变更。

## 9. 测试结果

- `.\mvnw.cmd "-Dtest=AiProviderDomainTest,AiProviderPropertiesTest,FakeAiProviderClientTest,OpenAiCompatibleUsageAdapterTest,OpenAiCompatibleChatClientTest" test`
  - 结果：通过；16 tests，0 failures，0 errors，0 skipped。
- 清除当前 Maven 进程的 `INFORMATION_HUB_TEST_DB_*` 与 `INFORMATION_HUB_TEST_EMPTY_DB_*` 后执行 `.\mvnw.cmd test`
  - 结果：通过；110 tests，0 failures，0 errors，19 skipped，即 91 tests 通过。
  - 19 个跳过项均为需要真实 MySQL 的既有数据库集成测试；Spring Application Context 与其余后端回归通过。
- `.\mvnw.cmd -DskipTests package`
  - 结果：通过；编译、资源复制、JAR 与 Spring Boot 重打包成功。
- Provider 单元测试全部使用 `MockRestServiceServer` 或 Fake，不访问公网，不需要真实 API Key。

## 10. 遗留问题

- 完整数据库集成测试需在 SSH 通道恢复并监听 `127.0.0.1:13306` 后重跑；本任务不修改数据库。
- TASK-026 负责业务 Prompt Assembly、最大响应大小、结构化 JSON 解析和 Definition Schema Validation；TASK-025 只返回 Assistant 原始文本与 Provider 元数据。
- TASK-027 及后续任务负责 Invocation 状态和 Usage 持久化、显式重试、Batch/Worker 与事务边界；本任务只提供错误和重试处置语义，不执行自动重试。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
