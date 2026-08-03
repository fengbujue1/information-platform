# TASK-025：实现 AI Provider、Usage Adapter 与 Fake Provider

状态：TODO  
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

- [ ] Fake Provider 可稳定返回结构化响应和 Usage
- [ ] OpenAI-compatible Adapter 可解析 Usage
- [ ] Provider 无 Usage 时字段 NULL/UNAVAILABLE
- [ ] API Key 不进入日志
- [ ] AI disabled 时不会发网络请求
- [ ] timeout/429/5xx 有明确分类
- [ ] ambiguous timeout 不无脑重试
- [ ] CI 不需要真实 Key
- [ ] 测试通过
- [ ] CURRENT_STATUS 指向 TASK-026

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
