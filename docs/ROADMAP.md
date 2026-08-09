# Information Platform Roadmap

## Phase 0：项目初始化

状态：已完成。

## Phase 1：BOSS 采集接入与归档

状态：已完成。

核心交付：

- BOSS Collector；
- InformationEnvelope V1；
- Information Hub；
- MySQL/Flyway；
- 幂等接入；
- 非破坏性合并；
- Snapshot；
- Outbox；
- Job Query API；
- 真实 E2E。

TASK-001 ～ TASK-010 已完成。

## Phase 2：标准化职位 Web 浏览 MVP

状态：已完成。

核心交付：

- Vue 3；
- Job API Client；
- 职位列表；
- 搜索/筛选/排序/分页；
- 职位详情；
- 历史 Snapshot；
- Session 保护；
- Browser E2E；
- Nginx 同源部署模板。

TASK-011 ～ TASK-019 已完成。

## Phase 3：AI Processing Foundation & User Relevance MVP

状态：**已完成。**

完成日期：2026-08-07。

### 核心交付

- Identity MVP；
- Prompt Profile / Prompt Version；
- Analysis Definition Registry；
- JOB_USER_RELEVANCE；
- Snapshot Input Projection；
- System Prompt / Output Schema；
- OpenAI-compatible Provider；
- Actual Provider Usage；
- Prompt Assembly；
- Structured Output Validation；
- Single Information Analysis；
- Candidate Resolver / Preview；
- Token Estimate；
- HMAC Confirm Token；
- Manual Batch；
- Budget Guard；
- MySQL Worker；
- Daily Schedule；
- Usage；
- Phase 3 Web；
- Fake Provider Full-stack E2E；
- 小规模真实 Provider 受控联调。

TASK-020 ～ TASK-032 已完成。

### Definition 演进

Phase 3 首个冻结版本：

```text
JOB_USER_RELEVANCE / 1
maxOutputTokens = 1000
```

真实 Provider 联调后兼容演进：

```text
JOB_USER_RELEVANCE / 2
maxOutputTokens = 5000
```

V1 保持历史兼容，V2 为当前版本。

### Phase 3 保持的边界

- JOB 只是 Information 的一种类型；
- Prompt 跟账号走并版本化；
- System Prompt / Schema 由平台控制；
- Analysis 绑定不可变 Snapshot；
- Actual Token 只来自 Provider Usage；
- Manual / Schedule 共用 Batch Engine；
- Provider / Worker 默认关闭；
- 不引入 Kafka、Redis、Elasticsearch、Vector DB、RAG、Agent 或微服务；
- 不实现自动推荐和通知。

## Phase 4：个性化推荐

状态：**进行中。**

设计状态：Accepted。

当前任务：`TASK-040`（尚未实施）；TASK-033～TASK-039（含 TASK-034A 数据模型通用化纠偏）已完成。

### 计划交付

- Recommendation Profile，并显式绑定 AI Prompt Profile；
- 预计算 Recommendation Run / Item；
- 确定性 scoring / ranking；
- deduplication / diversity / Top N；
- Analysis Batch 完成后的自动刷新；
- Manual Refresh；
- Feedback；
- BOSS contact disposition；
- Recommendation Feed；
- Phase 4 Web。

Phase 4 与 Phase 3 的区别：

- Phase 3：用户通过 Prompt 主动或定时发起 USER_RELEVANCE 分析；
- Phase 4：系统消费 Phase 3 已有成功 Analysis，基于 Recommendation Profile 生成预计算推荐。

Scope、Architecture、Data Model、Database Design、ADR、Contract 与 TASK-033 ～ TASK-044 已 Accepted。TASK-033～TASK-039（含 TASK-034A）已完成，当前采用 Generic Core + JOB Extension 并已提供 Recommendation Profile、View、Feedback、JOB disposition API、JOB Candidate Resolver 与 `JOB_RECOMMENDATION / V1` 确定性评分、Ranking / Deduplication / Diversity / Top N；后续从 TASK-040 起推进。

### 明确不做

- 独立 Recommendation Schedule；
- Recommendation 新增 AI 调用；
- Embedding / Vector DB / RAG；
- 自动读取 BOSS 聊天记录；
- 全站 UI/UX 重构；
- 生产部署与公网发布。

## Future：Retrieval / Semantic Search

根据真实数据规模再评估：

- Fulltext；
- Elasticsearch；
- Embedding；
- Vector Search；
- Hybrid Search；
- Rerank。

当前不提前引入。

## Future：Notification

推荐与事件模型稳定后再规划：

- 站内通知；
- 邮件；
- 短信；
- 微信；
- 其他推送。
