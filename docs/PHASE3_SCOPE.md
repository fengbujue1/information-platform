# Phase 3：AI Processing Foundation & User Relevance MVP

状态：Proposed  
规划日期：2026-08-02  
首个落地 Information Type：`JOB`  
首个落地 Analysis Purpose：`USER_RELEVANCE`

## 1. 定位

Information Platform 是通用的信息采集、归档、分析、推荐和浏览平台。当前只有职位信息，但长期会接入新闻、政策、房产、教育、行业报告等信息。因此 Phase 3 的 AI 基础设施不能绑定招聘业务。

Phase 3 的策略是：

> 通用骨架现在设计，只实现一个真实领域：JOB。

目标链路：

```text
User
→ Prompt Profile
→ Prompt Version
→ Analysis Definition
→ Candidate Resolver
→ Manual / Scheduled Trigger
→ Analysis Batch
→ AI Provider
→ Information Analysis
→ Token Usage
→ Web
```

## 2. Phase 3 必须完成

1. Identity MVP。
2. 用户级 Prompt Profile。
3. Prompt 不可变版本历史。
4. 通用 Analysis Definition。
5. Snapshot 级 Information Analysis。
6. OpenAI-compatible Provider 抽象。
7. Fake Provider。
8. Provider Usage 统一适配。
9. 单条分析。
10. 最近 N 天候选窗口 Preview。
11. 最大窗口、最大候选数和 Token Budget。
12. 异步 Batch。
13. 用户级 Actual Token 统计。
14. 每日定时分析。
15. Phase 3 Web。
16. 真实模型小规模验收。

## 3. 首个业务 MVP

第一版只实现：

```text
informationType = JOB
analysisPurpose = USER_RELEVANCE
analysisDefinition = JOB_USER_RELEVANCE_V1
```

用户可以创建自己的 Prompt Profile，例如：

```text
名称：远程 Java / 大数据

Prompt：
我关注 Java 后端、大数据和远程工作。
优先 Spring Boot、Spark、Hive 等方向。
如果明显是外包、销售、实施或必须现场办公，请明确提示。
```

系统针对候选职位的当前快照输出结构化“用户相关性分析”。

Phase 3 不把这个结果定义为推荐，不产生主动 Top N，也不发送通知。

## 4. 长期 AI 能力分层

### 4.1 ENRICHMENT

回答“这条信息本身是什么”。

例如职位分类、技能抽取、新闻主题、政策实体。Phase 3 只保留扩展位，不要求完成完整 ENRICHMENT。

### 4.2 USER_RELEVANCE

回答“根据当前用户明确配置的 Prompt，这条信息和用户当前关注点有多相关”。

这是 Phase 3 首个真实实现。

### 4.3 PROMPT_RETRIEVAL

回答“根据一次临时自然语言查询，从大规模历史信息中找出候选”。

Phase 3 不实现。未来再根据真实数据量评估 MySQL Fulltext、Elasticsearch、Embedding、Vector Search 或 Hybrid Search。

### 4.4 RECOMMENDATION

回答“基于长期画像和历史行为，系统应该主动推荐什么”。

继续留给后续阶段。

## 5. Identity MVP

Prompt、Analysis、Batch、Schedule 和 Usage 都跟账号走，因此 Phase 3 必须有最小身份能力。

只包含：

- 登录；
- 登出；
- 获取当前用户；
- 密码摘要；
- 会话；
- 当前用户数据隔离；
- 用户时区。

不包含：

- 公共自助注册；
- OAuth；
- 微信/短信登录；
- 找回密码；
- 组织；
- 多租户；
- 复杂 RBAC；
- 头像/社交资料。

推荐浏览器侧使用同源 Session Cookie；Collector Bearer Token 保持独立。

是否在 Phase 3 统一保护现有 `/api/v1/jobs/**`，由 TASK-020 根据真实代码和 E2E 冻结。

## 6. Prompt Profile 与 Prompt Version

一个用户可以有多个 Prompt Profile。

```text
User
├── Prompt Profile A
│   ├── V1
│   ├── V2
│   └── V3 Active
└── Prompt Profile B
```

保存修改时不 UPDATE 历史 Prompt 内容，而是创建新 Version。

每次 Analysis / Batch 必须保存真实使用的 `promptVersionId`。

用户只配置 User Prompt。以下内容由平台控制：

- System Prompt；
- Input Projection；
- Output Schema；
- Analysis Definition；
- Schema Version；
- Max Output Tokens。

## 7. Analysis Definition

第一版 Definition：

```text
key = JOB_USER_RELEVANCE
version = 1
informationType = JOB
purpose = USER_RELEVANCE
supportsUserPrompt = true
outputSchema = JobUserRelevanceV1
```

Definition 固定：

- Information Type；
- Purpose；
- 输入字段投影；
- System Prompt Version；
- Output Schema Version；
- Max Output Tokens；
- Candidate Resolver。

只实现 Job Definition，不提前创建 NEWS/HOUSE/POLICY 空实现。

## 8. Analysis 绑定 Snapshot

AI 分析必须绑定确定的信息版本：

```text
information_item
→ information_snapshot
→ information_analysis
```

逻辑分析身份至少包含：

```text
userId
snapshotId
promptVersionId
analysisDefinitionKey
analysisDefinitionVersion
```

Provider / Model 是实际执行元数据，默认不参与“是否已分析”的判定。切换模型不能静默触发全历史重跑。

## 9. AI 结果不得覆盖来源事实

不得 UPDATE：

- `information_item`；
- `job_information`；
- `information_snapshot`；
- `source_tags`；
- `source_skill_tags`；
- rawPayload。

AI 结果独立保存。

`source_skill_tags` 不等于 AI 标准技能。

## 10. JOB AI 输入

第一版只使用标准化字段和正文，不发送整个 rawPayload。

候选输入：

- title
- content
- companyName
- salaryText
- locationName
- cityName
- experienceText
- educationText
- remoteType
- jobStatus
- sourceTags
- sourceSkillTags
- welfare

最终字段由 Accepted Analysis Definition 冻结。

来源内容必须视为不可信数据，不能让职位正文中的文本覆盖 System Prompt。

## 11. 手动单条分析

流程：

```text
登录用户
→ 选择 Prompt Profile
→ 冻结 Active Prompt Version
→ 幂等检查
→ Token Estimate
→ Budget Guard
→ Analysis
→ AI Provider
→ Schema Validation
→ Result + Usage
```

相同逻辑分析身份已有 `SUCCEEDED` 时默认复用，不重复调用模型。

## 12. 最近 N 天 Preview

用户先输入：

```text
最近 [N] 天
```

Preview 不产生 AI 调用。

必须返回：

- windowStart / windowEnd；
- 窗口总数；
- eligible；
- 当前 Prompt Version 已分析数；
- 待分析数；
- item limit 影响；
- token budget 影响；
- 最终可执行数；
- Estimated Input / Output / Total Tokens；
- estimateMethod。

JOB 的“最近 N 天入库”语义为“首次进入 Information Platform 的时间”。具体映射哪个现有字段必须由 TASK-020 核实。

## 13. Batch 限制

同时限制：

```text
最大时间窗口
→ 最大候选数
→ 最大 Estimated Token Budget
```

平台有硬上限，用户不能突破。

候选过多时按稳定排序截取并记录 Deferred。

Token Budget 使用逐候选累加选择，超过预算的候选延期。

## 14. Estimated Token 与 Actual Token

Estimated 用于 Preview 和预算保护，可来自：

- 模型 Tokenizer；
- 兼容 Tokenizer；
- 保守估算算法。

必须记录 `estimateMethod`。

Actual 只能来自 Provider Response Usage。

如果 Provider 不返回 Usage：

```text
usageStatus = UNAVAILABLE
actualInputTokens = null
actualOutputTokens = null
actualTotalTokens = null
```

禁止把 Estimate 回填 Actual。

如果 Provider 已返回 Usage，但随后 JSON/Schema 校验失败，Analysis 可以 FAILED，但 Token 仍然必须记录。

## 15. Provider Invocation

每一次真实模型请求单独记录 Invocation。

原因：

- Analysis 可能有多个尝试；
- 失败调用也可能产生费用；
- 超时可能导致 Usage 不可知；
- 用户 Token 统计必须基于真实 Provider 调用。

Provider Invocation 是 Actual Token 的事实来源。

## 16. 用户级 Token 统计

Web 至少展示：

- 今日 Actual Token；
- 本月 Actual Token；
- 累计 Actual Token；
- 最近 Batch Token；
- Batch Estimated vs Actual；
- 单条 Analysis Token 明细。

不实现充值、套餐或账单系统。

如配置可靠价格，可展示 Estimated Cost / Derived Actual Cost；价格未知时保持 NULL。

## 17. 异步 Batch

不能用一个长 HTTP 请求同步跑完。

```text
Create Batch
→ 立即返回 batchId
→ 后台 Worker
→ Web 查询状态
```

第一版使用 MySQL 持久状态 + Spring 后台 Worker，不引入消息队列。

## 18. 手动 Batch

必须：

```text
Preview
→ 用户确认
→ Create Batch
```

创建时冻结：

- user；
- prompt profile/version；
- definition；
- windowStart / windowEnd；
- candidate selection；
- limits。

运行中新增数据不能自动加入该 Batch。

## 19. 定时分析

前端可以配置每日自动分析。

默认：

```text
enabled = false
localTime = 02:00
```

Schedule 保存：

- owner；
- Prompt Profile；
- Information Type；
- Definition；
- windowDays；
- maxCandidates；
- maxEstimatedTokens；
- local execution time；
- IANA timezone。

Schedule 绑定 Profile，不绑定 Version。触发时解析 Active Version，然后冻结到新 Batch。

## 20. Manual 与 Schedule 共用引擎

二者必须共享：

- Candidate Resolver；
- Idempotency；
- Token Estimate；
- Budget Guard；
- Batch Worker；
- Provider；
- Analysis；
- Usage；
- Schema Validation。

区别只有：

```text
triggerType = MANUAL
triggerType = SCHEDULED
```

## 21. Schedule 规则

Phase 3 MVP：

- 每日一次，不开放任意 Cron；
- 默认关闭；
- 默认 02:00；
- 使用 IANA timezone；
- 同一 Schedule 最多一个活动 Batch；
- 到点时上一 Batch 仍运行：跳过；
- 服务器停机错过：默认不补跑；
- 同 Schedule + scheduledFor 必须幂等；
- 候选超限允许部分延期；
- Token Budget 不允许绕过；
- 无候选记录可审计 No-op；
- 用户不在线也能以 Schedule Owner 身份执行。

## 22. Candidate Resolver 扩展

定义通用：

```text
CandidateResolver
```

Phase 3 只实现：

```text
JobCandidateResolver
```

未来再增加 News/House/Policy，不提前写空实现。

## 23. Web 范围

建议路由：

```text
/login
/ai/prompts
/ai/analyze
/ai/batches
/ai/batches/:id
/ai/schedules
/ai/usage
```

职位详情允许进入当前用户 Analysis。

## 24. Provider 配置

服务端配置：

```text
enabled
baseUrl
apiKey
model
timeout
maxOutputTokens
```

API Key 不进入 Git、前端、Prompt、Batch 或日志。

默认 AI `enabled=false`。

CI 只使用 Fake Provider。

## 25. 安全

- 用户只能访问自己的 AI 数据。
- AI 写操作必须认证。
- Session 模式处理 CSRF。
- Collector Token 与 User Session 分离。
- rawPayload 默认不发送模型。
- Source Content 作为不可信数据隔离。
- 普通 GET 不得暗中触发 AI。
- 前端不能直接调用 Provider。
- Schedule 不得绕过 Budget Guard。

## 26. 不在 Phase 3

- NEWS/HOUSE/POLICY 实际 AI 实现；
- 大规模 Prompt Retrieval；
- Embedding；
- Vector DB；
- RAG；
- Agent；
- Tool Calling 工作流；
- Kafka / RabbitMQ；
- Redis Queue；
- Elasticsearch；
- 微服务；
- 自动 Top N 推荐；
- 自动用户画像；
- 消息通知；
- 复杂 RBAC；
- 多租户；
- 充值/套餐/结算；
- 公网无保护开放。

## 27. 完成标准

- [ ] 用户可登录
- [ ] 多 Prompt Profile
- [ ] Prompt Version 历史
- [ ] Job User Relevance Contract
- [ ] Analysis 绑定 Snapshot
- [ ] 单条分析
- [ ] 最近 N 天 Preview
- [ ] 已分析/待分析数量
- [ ] Estimated Token
- [ ] Async Batch
- [ ] Candidate 与 Token Budget 限制
- [ ] Provider Actual Usage
- [ ] 今日/月度/累计 Usage
- [ ] Schedule 默认关闭
- [ ] Schedule 默认 02:00 本地时间
- [ ] Manual/Schedule 共用 Batch Engine
- [ ] Schedule 不重叠
- [ ] Misfire 默认不补跑
- [ ] Fake Provider 测试
- [ ] 真实模型小规模 E2E
- [ ] CI 无真实 API Key
- [ ] AI 不覆盖来源事实
- [ ] 不把 rawPayload 发给模型
- [ ] 不提前实现推荐和通知

## 28. TASK 顺序

```text
TASK-020  仓库审查与设计冻结
TASK-021  Identity MVP
TASK-022  Prompt Profile + Prompt Version
TASK-023  Analysis Definition 与 Contracts
TASK-024  Phase 3 数据模型 / Flyway
TASK-025  AI Provider + Usage Adapter + Fake Provider
TASK-026  Prompt Assembly + Structured Output
TASK-027  单条 Information Analysis
TASK-028  Candidate Resolver + Preview + Token Estimate
TASK-029  Async Batch + Budget Guard
TASK-030  Analysis Schedule
TASK-031  Phase 3 Web
TASK-032  Real Model + Full E2E Acceptance
```

## 29. TASK-020 必须确认的开放问题

- 是否统一给现有 Job Query API 加 Session Auth。
- 初始账号如何安全创建。
- Snapshot FK 和查询路径。
- “首次入库时间”真实字段。
- 现有 HTTP Client 是否复用。
- 是否已有 Spring Security。
- Scheduler 最小实现。
- Batch Worker 锁/事务。
- 默认最大窗口天数。
- 默认最大候选数。
- 默认 Estimated Token Budget。
- Token Estimate 算法。
- Job User Relevance 字段最终命名。
- 第一家真实 Provider。
