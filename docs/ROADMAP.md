# Information Platform Roadmap

## Phase 0：项目初始化

状态：已完成。

## Phase 1：BOSS 采集接入与归档

状态：已完成。

完成：
- BOSS 列表与详情采集；
- InformationEnvelope V1；
- Information Hub；
- MySQL/Flyway；
- 幂等接入；
- 历史快照；
- Outbox；
- Job Query API；
- 真实端到端验收。

## Phase 2：标准化职位 Web 浏览 MVP

状态：已完成。

完成：
- Vue 3；
- Job API Client；
- 职位列表；
- 筛选/排序/分页；
- 职位详情；
- 历史快照；
- URL 状态；
- Loading/Empty/Error/404；
- CI；
- Nginx 同源部署模板；
- Browser E2E；
- 真实环境验收。

TASK-011 ～ TASK-019 已完成。

## Phase 3：AI Processing Foundation & User Relevance MVP

状态：规划中。当前 Draft 等待 TASK-020 仓库审查和设计冻结。

### 目标

Phase 3 建立面向通用 Information 的 AI 基础设施，但只以 JOB 作为第一个真实实现。

首个 MVP：

```text
User
→ Prompt Profile / Version
→ JOB_USER_RELEVANCE_V1
→ 最近 N 天 Preview
→ Manual / Scheduled Batch
→ AI Provider
→ Information Analysis
→ Token Usage
→ Web
```

### Phase 3 TASK

1. [TASK-020：Phase 3 仓库审查与设计冻结](tasks/TASK-020.md)
2. [TASK-021：Identity MVP](tasks/TASK-021.md)
3. [TASK-022：Prompt Profile 与 Prompt Version](tasks/TASK-022.md)
4. [TASK-023：Analysis Definition 与 Contracts](tasks/TASK-023.md)
5. [TASK-024：Phase 3 数据模型与 Flyway](tasks/TASK-024.md)
6. [TASK-025：AI Provider、Usage Adapter 与 Fake Provider](tasks/TASK-025.md)
7. [TASK-026：Prompt Assembly 与结构化输出](tasks/TASK-026.md)
8. [TASK-027：单条 Information Analysis](tasks/TASK-027.md)
9. [TASK-028：Candidate Resolver、Preview 与 Token Estimate](tasks/TASK-028.md)
10. [TASK-029：异步 Analysis Batch 与 Budget Guard](tasks/TASK-029.md)
11. [TASK-030：每日 Analysis Schedule](tasks/TASK-030.md)
12. [TASK-031：Phase 3 Web](tasks/TASK-031.md)
13. [TASK-032：真实模型与 Phase 3 E2E 验收](tasks/TASK-032.md)

### Phase 3 核心边界

- JOB 只是首个 Information Type。
- 首个 Analysis Purpose = USER_RELEVANCE。
- Prompt 跟账号走并版本化。
- System Prompt / Schema 由平台控制。
- Analysis 绑定 Snapshot。
- Actual Token 以 Provider Invocation 为事实来源。
- Manual 与 Schedule 共用 Batch Engine。
- Schedule 默认关闭，默认用户本地 02:00。
- 不实现向量检索、推荐和通知。

## Phase 4：个性化推荐

状态：未开始。

可能包含：

- 用户画像；
- 主动匹配；
- 排序；
- 相似信息去重；
- 多样性；
- Top N；
- 推荐反馈。

Phase 4 不应与 Phase 3 的 USER_RELEVANCE 混淆：Phase 3 是用户明确配置 Prompt 后主动或定时运行；Phase 4 是系统基于长期画像主动产生推荐。

## Future：Prompt-driven Retrieval / Semantic Search

当真实信息类型和数据量需要时，再单独规划：

- 自然语言临时查询；
- Candidate Retrieval；
- Fulltext；
- Elasticsearch；
- Embedding；
- Vector Search；
- Hybrid Search；
- Rerank。

当前不提前选择检索基础设施。

## Future：Notification / Automation

在推荐和事件模型稳定后再规划：

- Web Notification；
- 邮件；
- 短信；
- 微信；
- 其他推送。
