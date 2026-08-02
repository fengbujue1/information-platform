# TASK-023：落地 Analysis Definition 与 Phase 3 Contracts

状态：TODO  
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

- [ ] Registry 可按 key/version 获取 Definition
- [ ] 仅 JOB_USER_RELEVANCE_V1 实现
- [ ] 输入字段与真实 Snapshot 数据一致
- [ ] System Prompt 有明确版本
- [ ] Output Schema 可程序校验
- [ ] 来源文本不能覆盖平台规则
- [ ] Score/Confidence 范围规则已测试
- [ ] Contract 与实现一致
- [ ] CURRENT_STATUS 指向 TASK-024

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

待填写。

## 9. 测试结果

必须填写实际执行命令和结果，禁止编造。

## 10. 遗留问题

待填写。

## 11. 完成确认

- [ ] 当前 TASK 文档已更新
- [ ] `docs/CURRENT_STATUS.md` 已更新
- [ ] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
