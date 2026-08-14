# TASK-029：实现异步 Analysis Batch 与 Budget Guard

状态：DONE
所属阶段：Phase 3
优先级：P0
负责人：User + Codex

## 1. 目标

实现 Manual Preview 确认后的异步 Batch，把候选集合冻结到 Batch Items，并通过数量和 Token Budget 安全执行。

## 2. 前置依赖

- TASK-028 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- Create Manual Batch。
- 校验 Preview/Confirm 一致性。
- Freeze promptVersion/window/candidates/limits。
- Batch / Batch Item 状态机。
- MySQL persistent worker。
- Worker 重启恢复。
- 逐项调用单条 Analysis 能力。
- already analyzed skip。
- maxCandidates。
- token budget candidate selection。
- 执行中 Actual Usage 汇总。
- 部分失败。
- Batch list/detail/progress API。
- 避免重复执行已成功 Item。
- 低并发或串行执行。

## 4. 不在本任务范围

- 不实现 Schedule。
- 不引入消息队列。
- 不做分布式 Worker。
- 不实现取消/暂停，除非真实实现非常简单且用户确认。
- 不做推荐。

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

- [x] HTTP Create Batch 快速返回 batchId
- [x] 候选集合被冻结
- [x] 新入库信息不加入已创建 Batch
- [x] maxCandidates 生效
- [x] estimated Token Budget 生效
- [x] Deferred 原因可见
- [x] Worker 重启不重复 SUCCEEDED
- [x] 单 Item 失败不回滚整 Batch
- [x] Actual Token 可按 Batch 聚合
- [x] PARTIAL_FAILED/FAILED/COMPLETED 语义正确
- [x] 定向测试和完整数据库回归通过
- [x] CURRENT_STATUS 指向 TASK-030

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

- 抽取 Preview/Confirm 共用的确定性候选解析结果；Confirm 使用 Token 冻结的 Owner、
  Profile/Prompt Version、Definition、绝对窗口、limits、统计、Estimate 与候选指纹重算，
  任一漂移均返回稳定冲突且不创建 Batch。
- 新增 `POST /api/v1/ai/analysis-batches/confirm`，只接收 `previewToken`，在单个
  `REPEATABLE_READ` 事务内原子创建 Batch 和 Candidate Limit 内的全部 Items，并返回
  HTTP 202。
- `(user_id, manual_request_id)` 同时处理顺序重复与并发 Confirm；并发唯一键失败事务回滚
  后读取胜出 Batch，不产生半批次。
- Token Budget 使用稳定候选前缀；预算内 Item 为 `SELECTED`，预算外 Item 为
  `DEFERRED/TOKEN_BUDGET`；Candidate Limit 外只保留 Batch 聚合计数。
- 没有可执行 Item 时统一为 `NOOP/NO_EXECUTABLE_ITEMS`；DEFERRED 不算失败；终态按
  `COMPLETED/PARTIAL_FAILED/FAILED` 的冻结规则派生。
- 扩展单条 Analysis 的内部执行边界：Worker 使用 Batch 冻结 Prompt Version、
  Definition Version 和 Snapshot，Invocation 显式绑定 `batch_item_id`；现有 Session 单条
  API 行为不变。
- 新增单并发 Spring/MySQL Worker，使用 `FOR UPDATE SKIP LOCKED`；领取和完成分别使用
  短事务，Provider HTTP 在事务外执行。
- 首次 Worker 轮询把崩溃遗留 RUNNING Invocation 转为 UNKNOWN，并令 Analysis/Item
  失败；不会对结果不确定的 Provider 请求自动重试。
- 新增 Batch list/detail/progress API；Actual Usage 只从绑定 Batch Item 的 Invocation
  聚合，并显式返回 Usage reported/unavailable 数量，Estimate 不回填 Actual。
- Worker 默认关闭；有可执行候选时，Confirm 要求 Worker 与 Provider 均已配置，避免产生
  无法处理的 PENDING Batch。
- 未新增或修改 Flyway；未实现 Schedule、前端、推荐、通知或新基础设施。

## 9. 测试结果

- `java -version`：Java 21.0.11。
- `.\mvnw.cmd -version`：Maven Wrapper 3.9.16，Java 21.0.11。
- `.\mvnw.cmd -DskipTests compile`：200 个主代码源文件编译成功。
- `.\mvnw.cmd "-Dtest=AnalysisBatchControllerTest,AnalysisBatchServiceTest,AnalysisBatchTransactionServiceTest,AnalysisBatchWorkerTest,AnalysisBatchWorkerTransactionServiceTest,AnalysisPreviewServiceTest,PreviewTokenServiceTest,InformationAnalysisServiceTest" test`
  ：19 项测试，0 失败、0 错误、0 跳过。
- `.\mvnw.cmd "-Dtest=*Test,!*IntegrationTest" test`：包含 Worker
  `COMPLETED/PARTIAL_FAILED/FAILED` 与重启 UNKNOWN 保护在内的 123 项非数据库测试，
  0 失败、0 错误、0 跳过。
- `.\mvnw.cmd -o -DskipTests package`：离线可执行 Spring Boot JAR 打包成功。
- `.\mvnw.cmd test`：SSH 隧道恢复后补跑成功；152 项测试，0 失败、0 错误、1 个独立
  空库条件测试因未配置 `INFORMATION_HUB_EMPTY_TEST_DB_URL` 跳过。

## 10. 遗留问题

- 完整真实 MySQL 回归已补跑；MySQL 8.4 仍有 Flyway 已测试版本提示。
- Schedule、Phase 3 Web 和真实 Provider E2E 分别属于 TASK-030～TASK-032。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
