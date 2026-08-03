# Phase 3 Codex 协作流程

当前状态：TASK-020 已于 2026-08-03 完成设计冻结。第一、二节保留为审查历史模板；后续从 TASK-024 开始。

## 1. 第一次：只做 TASK-020 仓库审查

把下面内容发送给 Codex：

```text
请恢复 Information Platform 最新项目上下文。

当前分支：dev

已知阶段：
- Phase 1 已完成。
- Phase 2 已完成。
- Phase 3 当前只有 Draft 规划，尚未开始业务实现。
- 当前任务是 docs/tasks/TASK-020.md。

本轮不要创建 Java 业务代码，不要创建 Flyway，不要安装 AI SDK，不要调用真实 AI。
你的职责是用本地真实仓库审查 Phase 3 Draft，并指出与真实代码不一致之处。

请按顺序阅读：

1. AGENTS.md
2. README.md
3. docs/PROJECT_CONTEXT.md
4. docs/ARCHITECTURE.md
5. docs/DATABASE_DESIGN.md
6. docs/ROADMAP.md
7. docs/CURRENT_STATUS.md
8. docs/PHASE3_SCOPE.md
9. docs/PHASE3_ARCHITECTURE_DRAFT.md
10. docs/PHASE3_DATA_MODEL_DRAFT.md
11. docs/DATABASE_DESIGN_PHASE3_DRAFT.md
12. docs/decisions/ADR-011-phase3-generic-ai-processing-boundary.md
13. docs/decisions/ADR-012-identity-prompt-version-and-analysis-ownership.md
14. docs/decisions/ADR-013-analysis-batch-budget-and-scheduling.md
15. docs/contracts/identity-mvp-v1.md
16. docs/contracts/ai-prompt-profile-v1.md
17. docs/contracts/information-analysis-v1.md
18. docs/contracts/analysis-batch-v1.md
19. docs/contracts/analysis-schedule-v1.md
20. docs/contracts/job-user-relevance-v1.md
21. docs/tasks/TASK-020.md
22. backend/information-hub/AGENTS.md
23. frontend/information-hub-web/AGENTS.md
24. 当前所有 Flyway migration
25. 当前 information/job/query 相关 PO、Mapper、Service、Controller、DTO
26. 当前 Web Router / API / auth 相关代码
27. 当前 Maven / npm 依赖
28. 当前 CI
29. 最近 10 条 commit

先执行：

git status
git log --oneline -10
git diff --check

然后只汇报，不修改文件：

一、当前真实系统能力。
二、Phase 3 Draft 与当前架构一致的部分。
三、与真实代码/DB 不一致的部分。
四、information_snapshot 的真实主键、字段、Mapper 和查询方式。
五、“首次进入平台”的真实时间字段应该用哪个。
六、现有项目是否已有 Spring Security。
七、现有 HTTP Client / Jackson 能复用什么。
八、Identity MVP 最小实现建议。
九、现有 Job Query API 是否应该在 Phase 3 纳入 Session Auth，以及对 E2E 的影响。
十、初始账号安全 Bootstrap 方案。
十一、Prompt Profile / Version 数据模型是否合理。
十二、Analysis 逻辑幂等键是否合理。
十三、Model Invocation 作为 Actual Token 事实源是否合理。
十四、Phase 3 候选 8 张表是否都必要；是否缺表或存在过度设计。
十五、逐表审查字段类型、NULL、默认值、PK、UNIQUE、INDEX、FK、ON DELETE。
十六、确认 information_item / information_snapshot 主键类型与 Phase 3 FK 兼容。
十七、Analysis UNIQUE 是否和失败重试/成功复用策略一致。
十八、Batch / Batch Item 是否足以冻结候选并支持 Worker 恢复。
十九、`UNIQUE(schedule_id, scheduled_for)` 是否满足 MySQL 幂等语义。
二十、Scheduler 查询索引是否合理。
二十一、是否需要 Spring Session JDBC 表；默认不提前增加。
二十二、是否需要 Preview 持久化表；默认不提前增加。
二十三、Manual Preview 冻结窗口的最小实现方案。
二十四、Token Estimate 第一版最合适的算法。
二十五、后台 Worker 的最小可靠方案。
二十六、每日 Schedule 的最小可靠方案。
二十七、平台默认 maxWindowDays / maxCandidates / maxEstimatedTokens 建议。
二十八、第一家 OpenAI-compatible Provider 的接入建议。
二十九、哪些 Draft 文件需要修改。
三十、哪些数据库设计项必须由我确认。
三十一、哪些其他决定必须由我确认。

数据库审查时必须明确对比：

docs/DATABASE_DESIGN.md
→ 当前 Accepted / 已实施数据库事实

当前 Flyway migration
→ 实际 SQL 事实

docs/PHASE3_DATA_MODEL_DRAFT.md
→ Phase 3 领域模型

docs/DATABASE_DESIGN_PHASE3_DRAFT.md
→ Phase 3 候选物理数据库设计

当前 PO / Mapper / Service
→ 实际 Java 映射和查询路径

必须遵守：
- Job 只是 Information 的一种类型。
- Phase 3 只真实实现 JOB，不提前实现其他领域。
- Prompt 跟账号走并版本化。
- System Prompt / Schema 由平台控制。
- Analysis 绑定 Snapshot。
- TASK-020 只审查数据库设计，不创建 SQL。
- TASK-024 只能实现 Accepted Database Design，不允许临场自由改表。
- Actual Token 只能来自 Provider Usage。
- Manual / Schedule 共用 Batch Engine。
- Schedule 默认关闭，默认用户本地 02:00。
- 不引入 Kafka、Redis、Elasticsearch、Vector DB、RAG、Agent、微服务。
- 不实现推荐和通知。

等待我确认后再修改 Phase 3 Draft。
```

## 2. TASK-020 审查后确认模板

```text
我确认继续 TASK-020。

请根据刚才的真实仓库审查结果，只修改 Phase 3 规划和长期文档，不实现业务代码。

要求：
1. 修正所有与真实代码/数据库不一致的 Draft。
2. 修正 `docs/DATABASE_DESIGN_PHASE3_DRAFT.md` 中的字段、类型、PK、UK、Index、FK、ON DELETE 和时间规则。
3. 把我已经确认的 Scope / ADR / Contracts / Phase 3 Data Model / Phase 3 Database Design 改为 Accepted。
4. 更新 PROJECT_CONTEXT / ARCHITECTURE / DATABASE_DESIGN / AGENTS 中确实需要长期同步的原则；注意 Phase 3 表尚未实施时，不要把 Draft 表伪装成已落地数据库事实。
5. 按已确认依赖调整为 TASK-020 → TASK-024 → TASK-021 → TASK-022 → TASK-023 → TASK-025～TASK-032。
6. CURRENT_STATUS 当前任务改为 TASK-024。
7. 更新 TASK-020 实施记录和实际文档校验结果。
8. 运行 git diff --check。
9. 不创建 Java、SQL、Vue 业务代码。
10. 不提交 Git。

完成后汇报：
- 修改文件；
- 冻结的决策；
- 仍开放的问题；
- 文档验证结果；
- TASK-021 开始前我需要知道的内容。
```

## 3. 通用任务启动模板

```text
请实施 docs/tasks/TASK-XXX.md。

开始前阅读：
1. AGENTS.md
2. docs/PROJECT_CONTEXT.md
3. docs/ARCHITECTURE.md
4. docs/DATABASE_DESIGN.md
5. docs/PHASE3_SCOPE.md
6. docs/PHASE3_DATA_MODEL_DRAFT.md
7. 如果任务涉及 DB/PO/Mapper/Analysis/Batch/Schedule，则阅读 docs/DATABASE_DESIGN_PHASE3_DRAFT.md
8. 与本 TASK 相关的 ADR / Contracts
9. docs/CURRENT_STATUS.md
10. 当前 TASK
11. 当前任务涉及模块的 AGENTS
12. 最近 8 条 commit

执行：
git status
git diff --check

不要立即修改。

先汇报：
1. 当前 TASK 目标。
2. 前置是否满足。
3. 真实现有代码可复用部分。
4. 计划修改文件。
5. 数据流。
6. 数据库/事务/锁影响。
7. 认证和 Owner 隔离。
8. Token/成本影响。
9. 测试计划。
10. 风险和待确认点。
11. 明确不在本 TASK 的内容。

等待我确认后实施。

约束：
- 只完成当前 TASK。
- 不覆盖来源事实。
- 不把 rawPayload 默认发给 AI。
- 不在前端或 Git 保存 API Key。
- Actual Token 只能来自 Provider Usage。
- 不把 Estimate 当 Actual。
- 不提前实现 NEWS/HOUSE/POLICY。
- 不提前实现推荐、通知、RAG、Vector DB、Kafka、Redis、Elasticsearch。
- 完成后更新 TASK 和 CURRENT_STATUS。
- 不提交 Git。
```

## 4. 数据库实现任务额外检查

TASK-024 或任何实际 schema 修改前，额外发送：

```text
本任务涉及 Phase 3 数据库实施。

请先对照：
- docs/DATABASE_DESIGN.md
- docs/PHASE3_DATA_MODEL_DRAFT.md
- docs/DATABASE_DESIGN_PHASE3_DRAFT.md
- 当前全部 Flyway migration
- 当前 PO / Mapper

必须遵守：
1. 只实现 TASK-020 已 Accepted 的 Database Design。
2. 不修改历史 migration。
3. 不擅自增加/删除表。
4. 不擅自修改字段语义、FK、UNIQUE、核心索引。
5. Actual Token 的事实来源必须保持 ai_model_invocation。
6. Schedule 幂等规则必须保持 Accepted 设计。
7. 如果设计与真实数据库冲突，STOP，不要自行修 SQL 绕过去。
8. 先说明冲突、影响、建议修改的设计文档，等我确认。

实施前请给出：
- 实际下一条 Flyway 版本号；
- 建表顺序；
- FK 创建顺序；
- 每张表索引；
- 对现有共享数据库的 migration 风险；
- 空库 migrate 测试；
- 已有库 upgrade 测试计划。
```

## 5. AI Provider 相关任务额外检查

```text
在 Provider 相关修改前，请额外确认：

- AI 默认是否 disabled。
- API Key 是否只从服务端配置读取。
- 日志是否可能打印 Authorization / API Key。
- Provider Usage 字段如何映射到统一模型。
- Provider 不返回 Usage 时是否保持 NULL。
- timeout 后结果不确定时是否会产生无脑自动 retry。
- Fake Provider 是否完全覆盖 CI。
- 测试是否会意外访问公网。

发现任何可能产生真实 API 费用的自动调用，请先停止并汇报。
```

## 6. Batch / Schedule 任务额外检查

```text
请额外检查：

- Preview 是否完全不调用 AI。
- windowStart/windowEnd 是否被冻结。
- Candidate 是否稳定排序。
- Batch 创建后候选是否冻结。
- maxCandidates 是否生效。
- Token Budget 是否生效。
- 已成功 Analysis 是否跳过。
- Worker 重启是否会重复收费。
- Manual / Scheduled 是否共用同一 Engine。
- Schedule 是否默认 disabled。
- Schedule 默认是否 02:00。
- timezone 是否 IANA。
- overlap 是否禁止。
- misfire 是否默认 skip。
- scheduleId + scheduledFor 是否幂等。
```

## 7. Token 统计审查模板

```text
请审查当前 Token Usage 实现。

必须区分：

Estimated:
- 来自 Tokenizer/估算算法；
- 用于 Preview / Budget。

Actual:
- 只能来自 Provider Response Usage；
- Provider 无 Usage 时必须 NULL/UNAVAILABLE；
- 不能用 Estimate 填充。

请检查：
1. 单次 Model Invocation。
2. Analysis 汇总。
3. Batch 汇总。
4. 今日 / 本月 / 累计 User Usage。
5. Provider 返回 Usage 但 Analysis 后续失败的场景。
6. timeout 导致 Usage unknown 的场景。
7. 重试产生多个 Invocation 的场景。
8. cached/reasoning token 可选字段。
9. 是否存在重复统计。
10. 是否有费用数据被误称为真实账单。
```

## 8. Scope 膨胀处理

```text
这是潜在的 Phase 3 范围变更，不要直接实现。

新增需求：
<填写>

请只评估：
- 是否属于 Phase 3；
- 是否破坏通用 Information AI 边界；
- 是否属于 Retrieval / Recommendation / Notification；
- 是否需要新 ADR；
- 是否需要新 Contract；
- 是否需要新 TASK；
- 对 Identity / Prompt / Batch / Token / Schedule 的影响；
- 是否引入新基础设施；
- 最小可行方案；
- 延后方案。

等待我确认。
```

## 9. 每个 TASK 收尾模板

```text
当前 TASK 准备收尾，请执行交接检查：

1. git status
2. git diff --check
3. Java / Python / Vue 当前相关测试
4. 检查是否超出 TASK
5. 检查秘密
6. 检查 Owner 隔离
7. 检查 rawPayload
8. 检查 Actual Token 事实来源
9. 更新 TASK 实施记录
10. 写实际测试命令和结果
11. 更新 CURRENT_STATUS
12. 明确下一 TASK
13. 不提交 Git

最后汇报：
- 修改文件；
- 完成功能；
- DB migration；
- API 行为；
- 测试；
- Token/费用影响；
- 安全影响；
- 未完成；
- 下一台电脑如何继续。
```

## 10. 用户提交

```powershell
git status
git diff --check
git diff
```

确认后：

```powershell
git add .
git diff --cached
git commit -m "feat(ai): complete task-xxx"
git push origin dev
```

纯文档任务建议：

```powershell
git commit -m "docs: freeze phase 3 ai processing"
```
