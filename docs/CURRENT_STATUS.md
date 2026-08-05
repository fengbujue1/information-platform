# 当前开发状态

更新时间：2026-08-05
当前分支：dev

## 当前阶段

- Phase 1 已完成。
- Phase 2 标准化职位 Web 浏览 MVP 已完成，并通过自动化浏览器测试和真实环境人工端到端验收。
- Phase 3 的 Scope、Architecture、Data Model、Database Design、ADR 和 Contracts 已通过 TASK-020 审查并 Accepted。
- TASK-024 已完成 Phase 3 V2 migration、8 张表、PO/Mapper 和真实 MySQL 验证。
- TASK-021 已完成 Identity MVP：受控账号 Bootstrap、密码摘要、同源 Session、CSRF、Login / Logout / Me、Owner 上下文，以及 Job Query/Web/E2E 的认证适配。
- TASK-022 已完成账号级 Prompt Profile、不可变 Prompt Version、Active Version 切换、Owner 隔离及对应 API。
- TASK-023 已完成通用 Analysis Definition Registry、唯一的 `JOB_USER_RELEVANCE_V1`、Snapshot 输入投影、版本化平台 System Prompt / Output Schema 与严格输出校验。
- TASK-025 已完成通用 AiProviderClient、OpenAI-compatible Adapter、统一 Usage/错误语义及 CI Fake Provider，真实 AI 默认 disabled。
- TASK-026 已完成稳定 Prompt Assembly、版本化执行上下文、1 MiB 响应上限、单一 JSON 解析和 Definition Schema 后处理。
- TASK-027 已完成单条 Information Analysis、逻辑身份幂等、Invocation attempt、Actual Usage、短事务执行和 Owner 隔离 API。
- TASK-028 已完成通用 Candidate Resolver、JOB 最近 N 天候选解析、无 AI 调用 Preview、Token Estimate、候选指纹和 10 分钟 HMAC 确认令牌。
- TASK-029 已完成 Manual Confirm、异步 Batch/Items、Budget Guard、串行 MySQL Worker、重启恢复、Actual Usage 聚合和 Owner 查询 API。
- Phase 3 Schedule 和对应 Web 业务实现尚未开始。

## 当前真实可用链路

```text
BOSS Collector
→ 列表与详情合并
→ InformationEnvelope V1
→ Information Hub
→ MySQL 当前版本与历史快照
→ Job Query API V1
→ Information Hub Web
```

当前已有：

- Spring Security 用户登录、登出和当前用户接口；
- 受控 Bootstrap 与安全密码摘要；
- 同源 Session、CSRF 和 Owner 上下文；
- 需要 Session 的 Job Query API 与 Web 登录入口；
- 保持独立 Bearer Token 语义的 Collector 接口。
- 需要 Session + CSRF 的 Prompt Profile / Version API。
- 默认禁用的 OpenAI-compatible Provider 基础、统一 Usage Adapter 和无网络 Fake Provider。
- 平台 System Prompt / Schema、User Prompt Version 与 Snapshot Input 的安全组装及严格结构化输出处理。
- 绑定不可变 Snapshot 与 Prompt Version 的单条 Analysis 执行及 Owner 查询 API。
- 需要 Session + CSRF 的 Analysis Batch Preview API，可返回候选规模、已分析/待分析计数、Token Estimate 汇总和短期确认令牌。
- 需要 Session + CSRF 的 Manual Batch Confirm API，以及 Batch list/detail/progress API。
- 默认关闭的单并发 Batch Worker；启用后使用 MySQL 短事务和 `SKIP LOCKED`，Provider HTTP 不持有事务。

当前没有：

- Schedule 的业务 Service/API；
- Prompt、Analysis、Batch、Schedule 的 Phase 3 Web 页面。

当前已经具备的 Phase 3 持久化基础：

```text
user_account
ai_prompt_profile
ai_prompt_version
information_analysis
ai_analysis_schedule
ai_analysis_batch
ai_analysis_batch_item
ai_model_invocation
```

其中 `user_account` 已由 Identity MVP 使用，`ai_prompt_profile` / `ai_prompt_version`
已由 TASK-022 业务 API 使用；`information_analysis` / `ai_model_invocation` 已由 TASK-027 使用；Batch 表已由 TASK-029 使用，Schedule 表尚待 TASK-030 暴露业务能力。

## 已冻结的 Phase 3 目标

```text
通用 Information AI Core
+
JOB_USER_RELEVANCE_V1
```

关键决策：

- JOB 只是 Information 的一种类型，Phase 3 只真实实现 JOB；
- Prompt 跟账号走并版本化，System Prompt/Schema 由平台控制；
- Analysis 绑定 `information_snapshot.id`；
- FIRST_INGESTED 使用 `information_item.first_seen_time`；
- Actual Token 只能来自 `ai_model_invocation` 的 Provider Usage；
- Manual/Schedule 共用 Batch Engine；
- Preview 使用 10 分钟 HMAC token，不建 Preview 表；
- 第一版使用内存 Session，不建 Spring Session JDBC 表；
- Job Query API 在 Identity MVP 后要求 Session，Collector Bearer Token 保持独立；
- Worker/Scheduler 使用 Spring + MySQL 短事务方案；
- Schedule 默认关闭、默认用户本地 02:00；
- 不引入 Kafka、Redis、Elasticsearch、Vector DB、RAG、Agent、微服务；
- 不实现推荐和通知。

## 当前任务

TASK-030：每日 Analysis Schedule。

TASK-029 已完成 Preview Confirm 一致性校验、Batch/Items 冻结、Manual 幂等、Budget Guard、串行 Worker、重启 UNKNOWN 保护和 Actual Usage 聚合。下一任务只应实现每日 Schedule 并复用现有 Batch Engine，不得修改 Worker 核心或提前实现 Phase 3 Web、推荐和通知。

## 下一步执行顺序

1. TASK-030：每日 Analysis Schedule；
2. TASK-031～TASK-032：按 Roadmap 继续。

任务编号不变；顺序调整是因为 Identity、Prompt 和 Analysis 的持久化依赖 TASK-024 的 Accepted 表结构。

## 当前风险

- Job Query/Web/E2E 已采用 Session 认证前置条件，后续测试必须继续使用认证夹具。
- Provider timeout 可能已计费但结果未知，后续 Worker 不得自动盲重试。
- Batch Worker 默认关闭；联调或部署必须同时启用 Worker 和 Provider，否则 Confirm 会拒绝创建有可执行 Item 的 Batch。
- 当前本机 `127.0.0.1:13306` SSH 隧道未开启，TASK-029 完整真实 MySQL 回归仍需在隧道恢复后补跑。
- Schedule 时区、DST、misfire 和 overlap 必须按 Accepted Contract 实现并测试。
- Provider API Key、Bootstrap 密码、Session Cookie 和 Collector Token 均不得进入 Git、前端或日志。
- MySQL 8.4 当前会触发 Flyway“最新已测试版本为 8.1”的提示；TASK-024 真实 migration 与回归均成功，后续依赖维护任务再评估升级。
- 数据库测试环境变量必须指向名称包含 `test` 且不包含 `dev` 的库；测试代码已强制校验，避免误迁移开发库。
