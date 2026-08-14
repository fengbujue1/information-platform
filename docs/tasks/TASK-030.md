# TASK-030：实现每日 Analysis Schedule

状态：DONE
所属阶段：Phase 3
优先级：P0
负责人：User + Codex

## 1. 目标

实现页面可配置的每日自动 AI 分析调度。默认关闭、默认用户本地时间 02:00，并严格复用 Manual Batch 的候选、幂等、Budget 和 Worker。

## 2. 前置依赖

- TASK-029 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- Schedule CRUD。
- enabled default false。
- localTime default 02:00。
- IANA timezone。
- windowDays/maxCandidates/maxEstimatedTokens。
- Prompt Profile 绑定。
- nextRunAt 计算。
- 后台 due schedule dispatcher。
- 运行时 Resolve Active Prompt Version。
- Scheduled Batch 创建。
- triggerType=SCHEDULED。
- scheduleId + scheduledFor 幂等。
- 同 Schedule overlap guard。
- misfire=SKIP。
- 无候选 No-op。
- Schedule list/detail。
- 上次/下次运行信息。
- Test Current Config 复用 Preview。
- DST / timezone 基础测试。

## 4. 不在本任务范围

- 不开放任意 Cron。
- 不支持小时/分钟级重复。
- 不补跑停机期间所有遗漏。
- 不引入 Quartz/XXL-JOB。
- 不绕过 Token Budget。
- 不直接从 Scheduler 调 Provider。

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

- [x] 新 Schedule 默认关闭
- [x] 默认本地时间 02:00
- [x] 用户主动开启后才运行
- [x] timezone 正确
- [x] Schedule 使用 Active Prompt Version
- [x] Batch 创建后 Prompt Version 冻结
- [x] 与 Manual 共用 Batch Worker
- [x] 同一 scheduledFor 不重复创建
- [x] overlap 被跳过
- [x] misfire 默认不补跑
- [x] 无候选不调用 AI
- [x] limits/budget 完全生效
- [x] 测试通过
- [x] CURRENT_STATUS 指向 TASK-031

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

- 新增 Owner 隔离的 Schedule list/create/detail/update/status/preview API；写操作继续使用
  Session + CSRF，客户端不能提交 `userId`。
- 新 Schedule 默认 `enabled=false`、本地 `02:00`，timezone 省略时使用账号 IANA 时区；
  停用时使用固定 SQL 显式写入 `next_run_at=NULL`。
- 新增 DST 安全的每日计划点计算：gap 向后移动到首个有效时间，overlap 选择较早
  offset，所有 `nextRunAt/scheduledFor` 按 UTC 持久化。
- Dispatcher 默认启用、每 30 秒扫描一次，通过
  `FOR UPDATE SKIP LOCKED` 每次锁定一个 due Schedule；Batch/NOOP 创建和
  `next_run_at` 推进位于同一 `REPEATABLE_READ` 事务。
- Schedule 以 `scheduledFor` 作为绝对候选窗口终点，触发时解析当前 Active Prompt
  Version 和 Definition Version，并冻结到 Scheduled Batch。
- Scheduled 与 Manual 共用 Candidate Resolver、Prompt Assembly、Token Estimate、
  Budget Guard、Batch Items 和现有 Worker；Scheduler 不直接调用 Provider。
- `(schedule_id, scheduled_for)` 同时通过应用查询和数据库唯一键保证幂等；
  overlap 使用 `NOOP/CONCURRENT_RUN`，超过 5 分钟的 misfire 使用
  `NOOP/MISFIRE`，不循环补跑历史。
- 无可执行候选继续使用 `NOOP/NO_EXECUTABLE_ITEMS`；Worker 或 Provider 未配置时
  使用 `NOOP/WORKER_DISABLED` 或 `NOOP/PROVIDER_UNAVAILABLE`，不留下无法消费的
  PENDING Batch。
- Schedule 列表/详情从最近 Scheduled Batch 派生 `lastRun`；Batch 查询新增
  `scheduleId/scheduledFor`，未新增 `lastTriggeredAt` 或 Schedule Run 表。
- 未新增或修改 Flyway；未修改 Worker 核心；未实现前端、真实模型 E2E、推荐、通知或
  新基础设施。

## 9. 测试结果

- `java -version`：Java 21.0.11。
- `.\mvnw.cmd -version`：Maven Wrapper 3.9.16，Java 21.0.11。
- `.\mvnw.cmd -DskipTests compile`：222 个主代码源文件编译成功。
- `.\mvnw.cmd "-Dtest=AnalysisScheduleTimeCalculatorTest,AnalysisScheduleServiceTest,AnalysisScheduleControllerTest,AnalysisScheduleTransactionServiceTest,AnalysisScheduleDispatcherTest,AnalysisSchedulePersistenceIntegrationTest,AnalysisBatchTransactionServiceTest,InformationHubApplicationTests" test`
  ：22 项最终定向测试，0 失败、0 错误、0 跳过；包含真实 MySQL due-row、显式
  `next_run_at=NULL` 和 scheduledFor 唯一键验证。
- `.\mvnw.cmd test`：171 项测试，0 失败、0 错误、1 个独立空库条件测试因未配置
  `INFORMATION_HUB_EMPTY_TEST_DB_URL` 跳过；`BUILD SUCCESS`。
- 完整回归后增加测试环境专用 `dispatcher-enabled=false`，避免自动化测试后台线程触发
  已持久化 Schedule；Dispatcher 本身继续通过显式单元和真实 Mapper 测试覆盖。

## 10. 遗留问题

- MySQL 8.4 仍会提示当前 Flyway 版本最高已测试到 MySQL 8.1；本任务 Migration 校验和
  真实数据库回归均成功。
- Profile 被外部状态变更为不可执行且无法满足 Batch 非空 FK 时，Dispatcher 安全推进
  日程并返回脱敏 `INVALID_CONFIGURATION`，不调用 Provider；第一版不新增 Schedule
  Run 表记录这种损坏配置。
- Phase 3 Web 和真实 Provider E2E 分别属于 TASK-031、TASK-032。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
