# TASK-026：实现 Prompt Assembly 与结构化输出校验

状态：DONE
所属阶段：Phase 3
优先级：P0
负责人：User + Codex

## 1. 目标

把平台 System Prompt、Analysis Definition、用户 Prompt Version 和标准化 Information Input 安全组装，并对模型输出进行严格 Schema Validation。

## 2. 前置依赖

- TASK-025 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- Job Input Projection。
- System Prompt + User Prompt + Source Data Assembly。
- Prompt injection 数据边界。
- Prompt/Definition/Schema 版本信息进入执行上下文。
- 结构化响应解析。
- JobUserRelevanceV1 Schema Validation。
- 最大响应大小。
- 非法 JSON、越界 score、超长数组/字符串错误处理。
- Fake Provider 测试。

## 4. 不在本任务范围

- 不实现 Batch。
- 不实现 Schedule。
- 不把 rawPayload 发模型。
- 不允许 User Prompt 覆盖 System Prompt/Schema。
- 不做推荐。

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

- [x] Prompt Assembly 可重复测试
- [x] 相同输入生成稳定结构
- [x] Source Data 与 Instruction 边界明确
- [x] rawPayload 未进入 Prompt
- [x] User Prompt 无法改变输出 Schema
- [x] 合法 Fake 响应通过
- [x] 非法 JSON 被拒绝
- [x] score/confidence 越界被拒绝
- [x] 测试通过
- [x] CURRENT_STATUS 指向 TASK-027

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

- 新增通用 `AnalysisPromptAssembler`，复用 Definition 的 Input Projection、System Prompt、Output Schema 和 `maxOutputTokens`，不绑定 JOB 持久化或 Controller。
- Provider Request 固定为三条消息：平台 System Prompt + Schema、JSON 转义后的 User Preferences、Definition 渲染的不可信 Source Data。
- 新增 `AssembledAnalysisPrompt`，冻结 Definition、System Prompt、Output Schema、Prompt Version、Snapshot 和 Information 的版本化执行元数据。
- User Prompt 和 Source Data 只进入 USER 消息，平台 Output Schema 只从 Definition 进入 SYSTEM 消息；用户输入不能替换平台 Schema。
- Source Content Boundary 统一使用 LF，保证不同操作系统上相同输入生成相同结构。
- 新增 `AnalysisOutputProcessor`，在 JSON 解析前执行 1 MiB UTF-8 响应上限，并要求响应包含一个且仅一个无重复字段的 JSON 值。
- 非法 JSON、重复字段、Markdown fence、尾随文本、第二个 JSON 和过大响应使用稳定脱敏错误拒绝，不保留可能回显原文的 Jackson cause。
- 解析成功后复用 Definition 的严格 Validator；只有合法 JSON 才进入 `ValidatedAnalysisOutput`，且返回防御性复制的 result JSON。
- 成功结果通过 `ValidatedAnalysisOutput` 保留原始 `AiProviderResult`；Schema 失败不修改调用方已持有的 Provider Result/Actual Usage，供 TASK-027 独立落库。
- 本任务没有数据库访问、事务、Analysis/Invocation 持久化、API、Batch、Schedule 或前端变更。

## 9. 测试结果

- `.\mvnw.cmd "-Dtest=AnalysisPromptAssemblerTest,AnalysisOutputProcessorTest,JobUserRelevanceDefinitionTest,JobUserRelevanceOutputValidatorTest,FakeAiProviderClientTest" test`
  - 结果：通过；18 tests，0 failures，0 errors，0 skipped。
- 在 `127.0.0.1:13306` 初始探测可连接且数据库测试环境变量存在时执行 `.\mvnw.cmd test`
  - 结果：未通过；SSH 通道在测试启动后中断，既有真实数据库测试均报 `Communications link failure / Connection refused`，与 TASK-026 代码无关。
- 清除当前 Maven 进程的真实数据库测试环境变量后再次执行 `.\mvnw.cmd test`
  - 结果：通过；118 tests，0 failures，0 errors，19 skipped，即 99 tests 通过。
  - 19 个跳过项均为需要真实 MySQL 的既有数据库集成测试；Spring Application Context 与其余后端回归通过。
- `.\mvnw.cmd -DskipTests package`
  - 结果：通过；编译、JAR 与 Spring Boot 重打包成功。
- TASK-026 测试全部使用 Fake Provider 或纯内存对象，不访问公网，不需要真实 API Key。

## 10. 遗留问题

- 完整数据库集成测试需在 SSH 通道稳定监听 `127.0.0.1:13306` 后重跑；本任务不修改数据库。
- 1 MiB 上限约束进入结构化后处理的 Assistant 文本；TASK-025 的 HTTP Adapter 仍先读取响应正文，若未来需要传输层流式限流应单独设计。
- Prompt injection 风险通过 SYSTEM/USER 角色隔离、JSON 转义、来源边界和服务端严格 Schema Validation 组合降低，不能宣称仅靠提示词彻底消除。
- TASK-027 负责 Snapshot/Prompt Version 数据库解析、Analysis/Invocation 状态、Provider 调用、Actual Usage 持久化和事务边界。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
