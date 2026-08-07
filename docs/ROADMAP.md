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

状态：未开始。

候选方向：

- 用户画像；
- 主动匹配；
- Recommendation Candidate；
- 排序；
- Top N；
- 相似信息去重；
- 多样性；
- 用户反馈。

Phase 4 与 Phase 3 的区别：

- Phase 3：用户通过 Prompt 主动或定时发起 USER_RELEVANCE 分析；
- Phase 4：系统基于长期用户画像主动产生推荐。

Phase 4 开始前必须单独完成 Scope、ADR、Contract 与 TASK 规划。

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
