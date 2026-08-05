# TASK-028：实现 Candidate Resolver、Preview 与 Token Estimate

状态：DONE
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

实现 JOB 最近 N 天候选解析和无 AI 调用的 Preview，让用户在分析前看到候选规模、已分析数量、待分析数量和预计 Token。

## 2. 前置依赖

- TASK-027 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- 定义通用 CandidateResolver 接口。
- 只实现 JobCandidateResolver。
- 使用 TASK-020 已冻结的 FIRST_INGESTED 真实字段。
- 固定 windowStart/windowEnd。
- 当前 Snapshot 解析。
- 已分析判断。
- stable ordering。
- platform maxWindowDays。
- maxCandidates。
- TokenEstimator。
- estimated input/output/total。
- estimateMethod。
- Preview API。
- Preview 防止候选集合漂移的确认机制设计。
- 单元/数据库测试。

## 4. 不在本任务范围

- 不创建 Batch。
- 不调用真实 AI Provider。
- 不实现 NEWS/HOUSE/POLICY Resolver。
- 不做向量搜索。
- 不做全文检索。
- 不做推荐排序。

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

- [x] 输入 N 天得到固定绝对窗口
- [x] 窗口语义与首次入库一致
- [x] total/eligible/alreadyAnalyzed/pending 正确
- [x] stable ordering 有测试
- [x] 超过 maxWindowDays 被拒绝
- [x] maxCandidates 影响可见
- [x] Estimated Token 有明确 method
- [x] Preview 不产生 Model Invocation
- [x] 结果可供 Manual Confirm 安全复用
- [x] CURRENT_STATUS 指向 TASK-029

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

- 定义通用 `CandidateResolver`、类型注册表、候选请求/结果领域对象，并只实现 `JobCandidateResolver`。
- JOB Resolver 使用 `information_item.first_seen_time` 的 UTC `[windowStart, windowEnd)` 语义，解析当前 Snapshot，按 `first_seen_time DESC, information_item.id DESC` 稳定排序，并排除相同 Owner、Snapshot、Prompt Version、Definition Key/Version 的 `SUCCEEDED` Analysis。
- Preview 固定一次绝对窗口，解析 Owner 所属 Profile 的 Active Prompt Version 和当前 Definition Version，返回窗口总数、eligible/already analyzed/pending、Candidate Limit 与 Token Budget 延后计数。
- Preview 编排显式使用只读 `REPEATABLE_READ` 事务，确保 Profile/Version、窗口统计与有序候选来自同一数据库快照；事务内无外部 HTTP 调用。
- 复用 TASK-027 的 `UTF8_BYTES_DIV3_MARGIN20_V1` Estimate，预算选择采用稳定前缀语义，Preview 不依赖 Provider Client，也不创建 Analysis、Invocation、Batch 或 Preview 记录。
- 对有序候选、Snapshot、Estimate 和预算决策计算版本化 SHA-256 指纹；使用 JDK `HmacSHA256` 签发 10 分钟令牌，冻结 Owner、版本、窗口、限制、计数、Estimate、指纹和 `manualRequestId`，并提供 TASK-029 可复用的验签/Owner 校验能力。
- 新增 `POST /api/v1/ai/analysis-batches/preview`，保持 Session + CSRF、Owner 隔离及稳定错误响应。
- 新增 `INFORMATION_HUB_PREVIEW_HMAC_SECRET` 配置，要求至少 32 个 UTF-8 字节；秘密不进入响应、数据库、Git 或日志。
- 未新增 Migration；Accepted V2 结构与索引已满足查询需要。

涉及模块：

- `analysis/candidate`：通用接口、JOB Resolver 与 MyBatis 查询；
- `analysis/preview`：限制、Estimate、指纹、HMAC Token、应用服务与 API；
- Prompt Mapper：增加 Owner 隔离的非锁定 Profile 查询；
- 配置、Contract、状态文档及对应测试。

## 9. 测试结果

- JDK 21.0.11 下执行 `./mvnw.cmd "-Dtest=AnalysisPreviewLimitsTest,CandidateFingerprintCalculatorTest,PreviewTokenServiceTest,AnalysisPreviewServiceTest,AnalysisPreviewControllerTest,JobCandidateResolverIntegrationTest" test`：15 项测试，0 失败、0 错误、0 跳过，真实 MySQL 候选解析集成测试通过。
- 使用相同真实 MySQL 测试环境执行 `./mvnw.cmd test`：142 项测试，0 失败、0 错误、1 跳过，构建成功。
- 唯一跳过项为需要独立空库变量的 `Phase3EmptySchemaMigrationIntegrationTest`；现有测试库上的 Flyway 校验、迁移与 Phase 3 Mapper 回归均通过。
- MySQL 8.4 仍显示既有 Flyway 支持版本提示，不影响本次验证结果。
- 当前环境直接执行系统 `mvn` 会命中 Maven 3.6.0/JDK 8，不满足项目要求；最终结果均使用仓库 Maven Wrapper 3.9.16 与显式 JDK 21 得到。
- 执行 `./mvnw.cmd -o -DskipTests package`：离线可执行 JAR 打包成功。

## 10. 遗留问题

- Manual Confirm、Batch/Item 创建、Budget Guard 执行和 Worker 属于 TASK-029，本任务只提供 Preview 及可复用 Token 验证边界。
- Schedule、Phase 3 Web、真实 Provider E2E、推荐与通知未实施。
- 运行环境在提供 Preview API 前必须配置 `INFORMATION_HUB_PREVIEW_HMAC_SECRET`；缺失或不足 32 字节时 Preview 返回服务不可用。
- 独立空库 Migration 测试仍需 `INFORMATION_HUB_TEST_EMPTY_DB_*` 环境变量，本次未配置。
- 用户检查、确认测试结果以及 commit/push 尚待完成。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
