# 当前开发状态

更新时间：2026-08-02  
当前分支：dev

## 当前阶段

Phase 1 已完成。

Phase 2 标准化职位 Web 浏览 MVP 已完成，并通过自动化浏览器测试和真实环境人工端到端验收。

Phase 3 已完成 Draft 规划包，尚未开始 AI 业务实现。

## 当前可用链路

```text
BOSS Collector
→ 列表与详情合并
→ InformationEnvelope V1
→ Information Hub
→ MySQL 当前版本与历史快照
→ Job Query API V1
→ Information Hub Web
```

## Phase 3 Draft 方向

```text
通用 Information AI Core
+
JOB USER_RELEVANCE MVP
```

包括：

- Identity MVP；
- Prompt Profile；
- Prompt Version；
- Analysis Definition；
- Snapshot 级 Analysis；
- OpenAI-compatible Provider；
- Model Invocation；
- Actual Token Usage；
- 最近 N 天 Preview；
- Async Batch；
- Token Budget；
- 每日 Schedule；
- Phase 3 Web。

## 当前任务

TASK-020：Phase 3 仓库审查与设计冻结。

TASK-020 只做：

- 真实仓库审查；
- Draft 与真实代码/DB 校正；
- Scope/ADR/Contracts 用户确认；
- 长期文档同步；
- 设计冻结。

TASK-020 不做：

- AI Java 业务实现；
- Flyway Phase 3 建表；
- Provider 调用；
- Login UI；
- Prompt UI；
- Batch；
- Schedule。

## 当前 Phase 3 关键原则

- JOB 只是 Information 的一种类型。
- 通用 AI Core 不绑定招聘。
- Phase 3 只真实实现 JOB。
- Prompt 跟账号走并版本化。
- 用户不能修改 System Prompt / Output Schema。
- Analysis 绑定 Snapshot。
- AI 不覆盖来源事实。
- rawPayload 不默认发送给模型。
- Actual Token 只能来自 Provider Usage。
- Estimate 与 Actual 分开。
- Manual 与 Schedule 共用 Batch Engine。
- Schedule 默认关闭。
- Schedule 默认用户本地 02:00。
- Schedule 不并发重叠。
- Misfire 默认不补跑。
- 不提前引入 Kafka、Redis、Elasticsearch、Vector DB、RAG、Agent、微服务。
- 不提前实现推荐和通知。

## 下一步

1. 用户将 Phase 3 Draft 文档提交到 dev。
2. 按 `docs/CODEX_PHASE3_WORKFLOW.md` 启动 TASK-020。
3. Codex 只做仓库审查。
4. 用户确认真实代码校正后的设计。
5. 将 Scope / ADR / Contracts 改为 Accepted。
6. TASK-020 完成后进入 TASK-021 Identity MVP。
