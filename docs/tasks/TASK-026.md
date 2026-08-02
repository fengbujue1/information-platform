# TASK-026：实现 Prompt Assembly 与结构化输出校验

状态：TODO  
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

把平台 System Prompt、Analysis Definition、用户 Prompt Version 和标准化 Information Input 安全组装，并对模型输出进行严格 Schema Validation。

## 2. 前置依赖

- TASK-025 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- Job Input Projection。
- System Prompt + User Prompt + Source Data Assembly。
- Prompt injection 数据边界。
- Prompt/Definition/Schema 版本信息进入执行上下文。
- 结构化响应解析。
- JobUserRelevanceV1 Schema Validation。
- 最大响应大小。
- 非法 JSON、越界 score、超长数组/字符串错误处理。
- Fake Provider 测试。

## 4. 不在本任务范围

- 不实现 Batch。
- 不实现 Schedule。
- 不把 rawPayload 发模型。
- 不允许 User Prompt 覆盖 System Prompt/Schema。
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

- [ ] Prompt Assembly 可重复测试
- [ ] 相同输入生成稳定结构
- [ ] Source Data 与 Instruction 边界明确
- [ ] rawPayload 未进入 Prompt
- [ ] User Prompt 无法改变输出 Schema
- [ ] 合法 Fake 响应通过
- [ ] 非法 JSON 被拒绝
- [ ] score/confidence 越界被拒绝
- [ ] 测试通过
- [ ] CURRENT_STATUS 指向 TASK-027

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
