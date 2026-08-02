# TASK-022：实现 Prompt Profile 与 Prompt Version

状态：TODO  
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

实现账号级可配置 Prompt，并保证 Prompt 每次修改都生成不可变版本，历史 Analysis 可以追溯到准确 Prompt Version。

## 2. 前置依赖

- TASK-021 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- `ai_prompt_profile` 和 `ai_prompt_version` 对应 Domain/PO/Mapper/Service/API。
- 当前用户多个 Prompt Profile。
- 新建 Profile。
- 新建 Prompt Version。
- Active Version 切换。
- Version 历史查询。
- Prompt content hash。
- Prompt 长度限制。
- Owner 隔离。
- 被引用 Version 不可物理修改。
- 前端 API types 可以在本任务只建立最小契约，不要求完整 UI。

## 4. 不在本任务范围

- 不实现完整 Prompt 管理页面。
- 不允许用户编辑 System Prompt。
- 不允许用户自定义 JSON Schema。
- 不实现 Analysis。
- 不调用 Provider。
- 不增加 JOB 专属 prompt 表。

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

- [ ] 一个用户可有多个 Profile
- [ ] 修改 Prompt 产生新 Version
- [ ] 旧 Version 内容不可修改
- [ ] Active Version 正确切换
- [ ] contentHash 稳定
- [ ] 用户 A 不能访问用户 B Prompt
- [ ] Prompt 长度限制生效
- [ ] 不存在 `user_account.prompt` 单字段捷径
- [ ] 测试通过
- [ ] CURRENT_STATUS 指向 TASK-023

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
