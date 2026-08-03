# TASK-023：落地 Analysis Definition 与 Phase 3 Contracts

状态：DONE
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

把通用 Analysis Definition 边界和 `JOB_USER_RELEVANCE_V1` 从文档落到可测试代码结构，冻结 Input Projection、System Prompt Version 和 Output Schema。

## 2. 前置依赖

- TASK-022 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- 建立 AnalysisDefinition Registry。
- 定义 informationType、analysisPurpose、definitionKey/version。
- 实现唯一真实 Definition：`JOB_USER_RELEVANCE_V1`。
- 对照真实 Job/Snapshot DTO 冻结输入字段。
- 冻结 Job User Relevance V1 输出字段。
- 定义 Schema Validation 规则。
- 定义 System Prompt 模板资源和版本。
- 定义来源内容“不可信数据”隔离方式。
- 定义 max output tokens。
- 更新 Contract 为实际代码一致状态。

## 4. 不在本任务范围

- 不调用真实 AI。
- 不创建 NEWS/HOUSE/POLICY Definition。
- 不实现 Batch。
- 不实现 Candidate Resolver 查询。
- 不实现推荐 Top N。
- 不做 Prompt Retrieval。

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

- [x] Registry 可按 key/version 获取 Definition
- [x] 仅 JOB_USER_RELEVANCE_V1 实现
- [x] 输入字段与真实 Snapshot 数据一致
- [x] System Prompt 有明确版本
- [x] Output Schema 可程序校验
- [x] 来源文本不能覆盖平台规则
- [x] Score/Confidence 范围规则已测试
- [x] Contract 与实现一致
- [x] CURRENT_STATUS 指向 TASK-025

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

- 新增通用 `AnalysisDefinition`、稳定 key/version 标识、Information Type、Analysis Purpose、Snapshot 输入边界与代码级 Registry。
- Registry 按 `JOB_USER_RELEVANCE` / `1` 查询，启动时拒绝重复 key/version，未知 Definition 立即失败。
- 仅实现 `JOB_USER_RELEVANCE_V1`，未创建 NEWS、HOUSE、POLICY 或其他 Definition。
- 输入投影固定为 Contract 的 13 个字段：标题/正文取不可变 Snapshot 冻结列，其余字段只取 `standardizedPayload.job`；缺失和显式 NULL 均保持 NULL。
- 输入边界类型不包含 `rawPayload`，也不接收 Collector Token、Cookie 或其他认证信息。
- System Prompt 与 JSON Output Schema 均使用 classpath 版本化资源，版本固定为 1，`maxOutputTokens=1000`。
- 来源输入先 JSON 序列化，再由平台添加 `<UNTRUSTED_SOURCE_DATA>` 边界；System Prompt 明确禁止执行来源数据中的指令，并要求缺失信息显式说明不足、所有信号均有输入事实依据。
- 输出校验严格拒绝缺失字段、未知字段、错误类型、越界 Score/Confidence、过长字符串和过大数组，并转换为不可变输出 Domain。
- 本任务没有数据库访问、事务、外部 HTTP 调用、API、Migration、Provider、Analysis 执行、Batch、Schedule 或前端变更。

## 9. 测试结果

- `.\mvnw.cmd "-Dtest=AnalysisDefinitionRegistryTest,JobUserRelevanceDefinitionTest,JobUserRelevanceOutputValidatorTest" test`
  - 结果：通过；11 tests，0 failures，0 errors，0 skipped。
- 清除当前 Maven 进程的 `INFORMATION_HUB_TEST_DB_*` 与 `INFORMATION_HUB_TEST_EMPTY_DB_*` 后执行 `.\mvnw.cmd test`
  - 结果：通过；94 tests，0 failures，0 errors，19 skipped，即 75 tests 通过。
  - 19 个跳过项均为需要真实 MySQL 的既有数据库集成测试；Spring Application Context、Identity、Prompt、Ingestion、Job Query 与 TASK-023 非数据库回归均通过。
- `.\mvnw.cmd -DskipTests package`
  - 结果：通过；编译、资源复制、JAR 与 Spring Boot 重打包成功。

## 10. 遗留问题

- 完整数据库集成测试需在 SSH 通道恢复并监听 `127.0.0.1:13306` 后重跑；最终审查时该端口未监听。本任务不修改数据库，当前未发现 TASK-023 代码遗留问题。
- TASK-025 及后续任务负责 Provider、Prompt Assembly、Analysis 执行、Candidate、Batch、Schedule 与 Web，本任务未提前实现。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
