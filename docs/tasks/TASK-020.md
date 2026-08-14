# TASK-020：Phase 3 仓库审查与设计冻结

状态：DONE
所属阶段：Phase 3
优先级：P0
负责人：User + Codex

## 1. 目标

用 Codex 对 Phase 3 Draft 进行真实仓库级审查，修正与现有代码、数据库和测试不一致的设计，并在用户确认后冻结 Phase 3 范围、ADR、Contracts 和 TASK 顺序。本任务不写 AI 业务代码。

## 2. 前置依赖

- Phase 2 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- 阅读根 AGENTS、PROJECT_CONTEXT、ARCHITECTURE、DATABASE_DESIGN、ROADMAP、CURRENT_STATUS。
- 阅读 Phase 3 Scope / Architecture Draft / Data Model Draft / Database Design Draft。
- 阅读 ADR-011～013、全部 Phase 3 Contracts。
- 检查现有 Maven 依赖、包结构、Security、HTTP Client、Flyway、Snapshot PO/Mapper/DTO、Web Router、CI。
- 核实 Snapshot 主键和读取方式。
- 核实 JOB “首次入库时间”的真实字段。
- 对照当前 `docs/DATABASE_DESIGN.md`、真实 Flyway、PO/Mapper 和 `docs/DATABASE_DESIGN_PHASE3_DRAFT.md` 做数据库专项审查。
- 核实 Phase 3 候选 8 张表是否真的全部必要。
- 逐表审查字段类型、NULL、默认值、PK、UNIQUE、INDEX、FK、ON DELETE 和时间语义。
- 核实 `information_item.id`、`information_snapshot.id` 的真实类型和 Phase 3 FK 兼容性。
- 核实 Prompt Profile / Version 的 Active Version 关系和删除规则。
- 核实 Analysis 逻辑唯一键与失败重试策略是否冲突。
- 核实 `ai_model_invocation` 是否足以作为 Actual Token 的唯一事实来源。
- 核实 Batch/Batch Item 是否能冻结候选并支持 Worker 恢复。
- 核实 `(schedule_id, scheduled_for)` 的 MySQL 幂等语义。
- 核实 Scheduler 查询所需索引。
- 核实是否需要 Spring Session JDBC 表或 Preview 持久化表，默认不提前增加。
- 评估 Identity MVP 对现有 Job Query API 和 E2E 的影响。
- 评估初始账号安全 Bootstrap 方案。
- 评估 Spring 后台 Worker 和 Schedule 的最小实现。
- 给出平台 hard limits 建议。
- 给出 Token Estimate 第一版方案。
- 给出第一家真实 OpenAI-compatible Provider 实现建议。
- 修正文档冲突。
- 用户确认后将 Scope / ADR / Contracts / Phase 3 Data Model / Phase 3 Database Design 改为 Accepted。
- 更新长期 ARCHITECTURE / PROJECT_CONTEXT / DATABASE_DESIGN / AGENTS 中确实需要同步的 Accepted 原则。
- 将 CURRENT_STATUS 当前任务更新为 TASK-024。

## 4. 不在本任务范围

- 不创建 `user_account` 表。
- 不新增 Spring Security 依赖。
- 不创建 AI Java package。
- 不创建 Flyway SQL。
- 不调用真实 AI。
- 不实现前端 Login。
- 不开始 TASK-021。

## 5. 实施原则

- 只完成当前 TASK。
- 不覆盖来源事实。
- 不把 rawPayload 默认发送给模型。
- 不把秘密写入 Git 或前端。
- 不提前引入 Kafka、Redis、Elasticsearch、向量数据库、RAG 或微服务。
- 不提前实现推荐和通知。
- Codex 修改前必须先汇报计划并等待确认。
- Codex 不提交 Git，由用户检查后提交。

## 6. 验收标准

- [x] Snapshot 绑定方案已用真实代码验证
- [x] FIRST_INGESTED 时间语义已映射真实字段
- [x] Identity 保护范围已确认
- [x] 初始账号 Bootstrap 方案已确认
- [x] Token Estimate 方案已确认
- [x] Batch/Worker/Scheduler 最小实现已确认
- [x] 平台默认 hard limits 已确认
- [x] 第一家 Provider 方案已确认
- [x] Phase 3 候选表数量已确认
- [x] 8 张候选表的字段类型、NULL、PK、UK、Index、FK、ON DELETE 已逐项审查
- [x] Analysis 唯一键与重试策略已确认
- [x] Invocation Actual Token 事实源已确认
- [x] Batch/Schedule 幂等和索引已确认
- [x] 不增加 Session/Preview 附加表
- [x] 所有 Phase 3 Accepted 文档无已知相互冲突
- [x] Scope / ADR / Contracts / Data Model / Database Design 经用户确认后 Accepted
- [x] 未产生 AI 业务代码
- [x] CURRENT_STATUS 指向 TASK-024

## 7. 实施前必须汇报

- 当前真实代码与文档基线；
- 前置依赖是否满足；
- 计划修改文件；
- 数据流 / 事务 / 安全边界；
- 测试计划；
- 风险；
- 与 Draft 设计不一致的地方；
- 明确不实施的内容。

## 8. 实施记录

2026-08-03：

- 对照 V1 Flyway、Information/Snapshot PO/Mapper/Service、Job Query、Web Router、依赖和 CI 完成真实仓库审查。
- 确认 Snapshot 主键为 `BIGINT UNSIGNED`，当前 Snapshot 通过 `(information_id, current_version_no)` 查询。
- 将 FIRST_INGESTED 固定为 `information_item.first_seen_time`。
- 冻结 8 张表及全部字段、NULL/default、PK/UK/Index/FK/ON DELETE。
- 冻结 Analysis 重试、Invocation Usage、Manual/Schedule 幂等、Worker 与 Scheduler 方案。
- 冻结 Session Auth、CSRF、一次性账号 Bootstrap、Preview HMAC、Estimate 和平台硬上限。
- 因 Identity/Prompt 依赖数据库表，将实际执行顺序调整为 TASK-024 → TASK-021 → TASK-022 → TASK-023 → TASK-025。
- 只修改文档，未创建 Java、Vue、SQL、Flyway 或 AI 调用。

## 9. 测试结果

实际执行：

```text
git -c safe.directory=E:/1.project/information-platform status --short --branch
git -c safe.directory=E:/1.project/information-platform log --oneline -10
git -c safe.directory=E:/1.project/information-platform diff --check
```

结果：

- 开始实施前工作树干净，分支为 `dev`，最新提交为 `2a9255e`。
- 实施前与完成后 `git diff --check` 均通过；仅输出 Windows Git 的 LF→CRLF 工作树提示，无 whitespace error。
- 修改过的 Markdown 相对链接检查通过。
- 一致性检索未发现 Phase 3 核心文档仍处于 Proposed/Draft，未发现待 TASK-020 决定的残留；`ADR_TEMPLATE.md` 的状态示例除外。
- 新增内容秘密模式扫描无命中。
- 本任务只有 Markdown 文档变更，未运行 Java/Vue 测试。

## 10. 遗留问题

- TASK-024 尚未执行，因此 8 张 Phase 3 表仍不是当前数据库事实。
- 第一家真实 Provider 的账号、模型名称与服务端秘密由 TASK-032 E2E 前由用户配置，不进入 Git。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
