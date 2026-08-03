# 当前开发状态

更新时间：2026-08-03
当前分支：dev

## 当前阶段

- Phase 1 已完成。
- Phase 2 标准化职位 Web 浏览 MVP 已完成，并通过自动化浏览器测试和真实环境人工端到端验收。
- Phase 3 的 Scope、Architecture、Data Model、Database Design、ADR 和 Contracts 已通过 TASK-020 审查并 Accepted。
- Phase 3 业务代码和数据库 migration 尚未开始。

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
- Phase 3 数据表；
- AI Provider；
- Analysis/Batch/Schedule；
- Phase 3 Web 页面。

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

TASK-024：实现 Phase 3 数据模型与 Flyway。

TASK-024 只允许按 Accepted `docs/DATABASE_DESIGN_PHASE3_DRAFT.md` 实施：

- 为现有 `information_item` 增加 FIRST_INGESTED 查询索引；
- 新增 8 张 Phase 3 表；
- 实现冻结的 PK/UK/Index/FK/ON DELETE/default；
- 新增对应 PO/Mapper 和数据库测试；
- 验证空库 migrate 与已有库 upgrade；
- 将真正实施的数据库事实合并到 `docs/DATABASE_DESIGN.md`。

TASK-024 不允许临场重新设计表结构，也不实现登录、Prompt API、Provider、Batch Worker 或 Schedule。

## 下一步执行顺序

1. TASK-024：Phase 3 数据模型与 Flyway；
2. TASK-021：Identity MVP；
3. TASK-022：Prompt Profile 与 Prompt Version；
4. TASK-023：Analysis Definition 与 Contracts 代码落地；
5. TASK-025：AI Provider 与 Fake Provider；
6. TASK-026～TASK-032：按 Roadmap 继续。

任务编号不变；顺序调整是因为 Identity、Prompt 和 Analysis 的持久化依赖 TASK-024 的 Accepted 表结构。

## 当前风险

- 8 张 Phase 3 表尚未迁移，不能把 Accepted 设计误认为现有数据库事实。
- Identity 会改变现有 Job Query/Web/E2E 的访问前置条件，TASK-021 必须同时更新测试登录夹具。
- Provider timeout 可能已计费但结果未知，后续 Worker 不得自动盲重试。
- Schedule 时区、DST、misfire 和 overlap 必须按 Accepted Contract 实现并测试。
- Provider API Key、Bootstrap 密码、Session Cookie 和 Collector Token 均不得进入 Git、前端或日志。
