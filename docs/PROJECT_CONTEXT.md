# Information Platform 项目背景

## 项目目标

建设一个通用的信息采集、归档、AI 分析、推荐和浏览平台。

系统从 BOSS 职位信息开始，但长期领域模型不绑定招聘业务。未来可扩展到 NEWS、GOVERNMENT、HOUSE_PRICE、EDUCATION 等信息类型。

## 已完成阶段

截至 2026-08-09：

- Phase 0：完成；
- Phase 1：完成；
- Phase 2：完成；
- Phase 3：完成；
- Phase 4：进行中，TASK-033～TASK-043（含 TASK-034A）已完成，当前任务为 TASK-044（尚未实施）。

## 当前已落地核心流程

```text
Collector
→ Information Hub
→ 归档与 Snapshot
→ Web 浏览
→ User Prompt
→ USER_RELEVANCE Analysis
→ Manual / Scheduled Batch
→ Provider Usage
→ Phase 3 Web
```

Phase 4 Recommendation 规划基线已 Accepted，运行日志基线、V4 Generic Recommendation Core + JOB Extension 持久化骨架、Recommendation Profile 与 Interaction Backend/API、JOB Candidate Resolver、`JOB_RECOMMENDATION / V1` 确定性评分、Ranking / Deduplication / Diversity / Top N、Recommendation Run / Worker / Manual Refresh、Analysis Batch Auto Trigger、Recommendation Feed Query API，以及 Phase 4 Web 已实施；真实全栈 E2E 与 Notification 尚未开始。

## 长期核心流程

```text
Collector
→ Information Hub
→ 归档与标准化
→ 规则过滤
→ AI 分析
→ 个性化推荐
→ Web / Notification
```

## 长期设计原则

1. Collector 与 Information Hub 解耦。
2. Collector 不直接连接主数据库。
3. Job 只是 Information 的一种类型。
4. 原始数据必须保留。
5. 标准化字段不能取代原始数据。
6. AI 分析结果不能覆盖来源事实。
7. 信息质量与用户相关性分开。
8. 新数据源通过统一协议接入。
9. 当前保持模块化单体，出现真实瓶颈后再考虑拆分。
10. Prompt、Snapshot、Definition Version 与 Provider Invocation 必须可追溯。
11. Provider / Worker 等可能产生费用或副作用的能力默认关闭。

## 当前技术方向

- Collector：Python
- Backend：Java 21、Spring Boot、Maven Wrapper
- Persistence：MySQL、Flyway、MyBatis-Plus
- Frontend：Vue 3、TypeScript、Vite、Element Plus
- Deployment：Docker Compose / Nginx
- AI：OpenAI-compatible Provider，Spring `RestClient` + Jackson

## Phase 3 已落地

### Identity

- Bootstrap；
- Session；
- CSRF；
- Owner 隔离。

### Prompt

- Prompt Profile；
- immutable Prompt Version；
- Active Version；
- contentHash 复用。

### Analysis

- `AnalysisDefinition`；
- Snapshot 绑定；
- Prompt Version 绑定；
- Definition Version 绑定；
- Single Analysis；
- Invocation / Actual Usage。

### JOB USER_RELEVANCE

```text
V1:
analysisDefinitionVersion = 1
maxOutputTokens = 1000

V2:
analysisDefinitionVersion = 2
maxOutputTokens = 5000
```

V1 作为历史版本保留，V2 为当前版本。

### Batch / Schedule

- Preview；
- Candidate Limit；
- Token Budget；
- HMAC Confirm；
- Manual Batch；
- Worker；
- Daily Schedule；
- Scheduled Batch；
- Usage aggregation。

### Web

- Login；
- Job；
- Prompt；
- Analysis；
- Batch；
- Schedule；
- Usage。

## 当前暂不引入

- 微服务；
- Kafka / RabbitMQ；
- Redis；
- MongoDB；
- Elasticsearch；
- 向量数据库；
- RAG / Agent；
- Kubernetes；
- Notification；
- 复杂用户权限。

## 当前阶段

Phase 3 已完成并归档。

Phase 4：Personalized Recommendation MVP 已启动，当前任务为 TASK-044（尚未实施）。Scope、ADR、Contract 与 TASK 规划已 Accepted；Backend Operational Logging Baseline、V4 Generic Core + JOB Extension 持久化骨架、Recommendation Profile、Interaction API、JOB Candidate Resolver、`JOB_RECOMMENDATION / V1` 确定性评分、Ranking / Deduplication / Diversity / Top N、Recommendation Run / Worker / Manual Refresh、Analysis Batch Auto Trigger、Recommendation Feed Query API，以及 Phase 4 Web 已实施；真实全栈 E2E、验收与收尾尚未实施。
