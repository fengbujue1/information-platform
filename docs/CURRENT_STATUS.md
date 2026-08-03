# 当前开发状态

更新时间：2026-08-03
当前分支：dev

## 当前阶段

- Phase 1 已完成。
- Phase 2 标准化职位 Web 浏览 MVP 已完成，并通过自动化浏览器测试和真实环境人工端到端验收。
- Phase 3 的 Scope、Architecture、Data Model、Database Design、ADR 和 Contracts 已通过 TASK-020 审查并 Accepted。
- TASK-024 已完成 Phase 3 V2 migration、8 张表、PO/Mapper 和真实 MySQL 验证。
- Phase 3 Identity、Prompt、Provider、Analysis Engine、Batch Worker、Schedule 和 Web 业务实现尚未开始。

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

当前没有：

- Spring Security 用户登录；
- AI Provider；
- Identity、Prompt、Analysis、Batch、Schedule 的业务 Service/API；
- Phase 3 Web 页面。

当前已经具备但尚未暴露业务 API 的 Phase 3 持久化基础：

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

TASK-021：Identity MVP。

TASK-021 应在 TASK-024 已实施的 `user_account` 表和 Mapper 基础上完成最小账号登录、Bootstrap、同源 Session、CSRF、Owner 上下文和现有 Job Query/Web/E2E 认证适配。不得提前实现 Prompt、Provider、Analysis Engine、推荐或通知。

## 下一步执行顺序

1. TASK-021：Identity MVP；
2. TASK-022：Prompt Profile 与 Prompt Version；
3. TASK-023：Analysis Definition 与 Contracts 代码落地；
4. TASK-025：AI Provider 与 Fake Provider；
5. TASK-026～TASK-032：按 Roadmap 继续。

任务编号不变；顺序调整是因为 Identity、Prompt 和 Analysis 的持久化依赖 TASK-024 的 Accepted 表结构。

## 当前风险

- Identity 会改变现有 Job Query/Web/E2E 的访问前置条件，TASK-021 必须同时更新测试登录夹具。
- Provider timeout 可能已计费但结果未知，后续 Worker 不得自动盲重试。
- Schedule 时区、DST、misfire 和 overlap 必须按 Accepted Contract 实现并测试。
- Provider API Key、Bootstrap 密码、Session Cookie 和 Collector Token 均不得进入 Git、前端或日志。
- MySQL 8.4 当前会触发 Flyway“最新已测试版本为 8.1”的提示；TASK-024 真实 migration 与回归均成功，后续依赖维护任务再评估升级。
- 数据库测试环境变量必须指向名称包含 `test` 且不包含 `dev` 的库；测试代码已强制校验，避免误迁移开发库。
