# Phase 3 完成总结

状态：Completed
完成日期：2026-08-07
阶段：AI Processing Foundation & User Relevance MVP

## 1. 完成结论

Phase 3 已完成。

Information Platform 已从 Phase 1 / Phase 2 的：

```text
采集
→ 归档
→ Snapshot
→ Query API
→ Web 浏览
```

扩展为：

```text
User
→ Prompt
→ Snapshot
→ AI Analysis
→ Preview
→ Batch
→ Schedule
→ Provider Usage
→ Web
```

Phase 3 建立的是通用 AI Processing Foundation，而不是只面向招聘业务的单一 AI Service。

## 2. 核心交付

### Identity

- Bootstrap；
- Login / Logout / Me；
- Session；
- CSRF；
- Owner 隔离。

### Prompt

- Prompt Profile；
- immutable Prompt Version；
- Active Version；
- contentHash 复用。

### Analysis Definition

- 通用 `AnalysisDefinition<I,O>`；
- Registry；
- Snapshot Input Projection；
- System Prompt；
- Output Schema；
- Output Validator。

### Provider

- OpenAI-compatible Client；
- Provider Usage；
- Fake Provider；
- timeout / retry semantics；
- 安全配置。

### Information Analysis

- Snapshot 绑定；
- Prompt Version 绑定；
- Definition Version 绑定；
- 幂等；
- explicit retry；
- Invocation attempt；
- Actual Usage；
- structured result。

### Preview / Batch

- Candidate Resolver；
- 最近 N 天；
- Token Estimate；
- HMAC Confirm；
- Candidate Limit；
- Token Budget；
- Manual Batch；
- Worker；
- 重启恢复。

### Schedule

- Daily Schedule；
- timezone；
- due Dispatcher；
- Scheduled Batch；
- overlap / misfire / idempotency。

### Web

- Login；
- Jobs；
- Prompt；
- Analysis；
- Preview；
- Batch；
- Schedule；
- Usage。

## 3. Definition 版本演进

### V1

```text
JOB_USER_RELEVANCE / 1
maxOutputTokens = 1000
```

### V2

```text
JOB_USER_RELEVANCE / 2
maxOutputTokens = 5000
```

V2 是真实 Provider 联调后的兼容演进。

没有修改 V1，因此历史 Analysis 和冻结 Batch 仍可按原 Version 解析。

V2 继续复用 V1 的：

- Input Projection；
- System Prompt V1；
- Output Schema V1；
- Validator；
- Source Content Boundary。

## 4. 核心设计结果

Phase 3 最重要的工程结果不是“调用了一个大模型”，而是建立了以下可追溯关系：

```text
User
+
Prompt Version
+
Information Snapshot
+
Analysis Definition Version
+
Model Invocation
```

因此系统能够回答：

- 谁执行了分析；
- 使用哪版用户偏好；
- 分析哪一版来源事实；
- 使用哪版平台规则；
- 实际调用了哪个 Provider / Model；
- 实际产生多少 Token；
- 失败是否已经产生 Provider Usage。

## 5. 安全边界

最终保持：

- Provider 默认 disabled；
- Worker 默认 disabled；
- Schedule 默认 disabled；
- API Key 只在服务端；
- Owner 只从认证上下文取得；
- rawPayload 不默认进入模型；
- AI 结果不覆盖来源事实；
- Actual Token 只来自 Provider Usage；
- Provider HTTP 不持有数据库事务；
- timeout / ambiguous failure 不自动盲重试。

## 6. Phase 3 未包含

- Recommendation；
- User Profile；
- Top N；
- Notification；
- RAG；
- Embedding；
- Vector DB；
- Elasticsearch；
- Kafka / Redis；
- 微服务。

这些不属于 Phase 3 遗留，而是明确的非范围项。

## 7. CI 处理

CI 后续作为独立工程维护事项处理。

本次 Phase 3 阶段关闭不以 CI 状态作为阻塞条件。

## 8. 后续阶段

Phase 4 尚未开始。

建议进入 Phase 4 前先进行一次 Phase 3 数据复盘：

- 用户 Prompt 的实际使用方式；
- Estimate / Actual Token 偏差；
- relevanceScore 分布；
- Analysis 成功率；
- Provider timeout / failure 情况；
- Batch / Schedule 使用频率；
- 用户真正需要的推荐行为。

基于真实使用数据，再决定 Phase 4 推荐模型和数据结构。
