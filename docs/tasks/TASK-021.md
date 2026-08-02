# TASK-021：实现 Identity MVP

状态：TODO  
所属阶段：Phase 3  
优先级：P0  
负责人：User + Codex

## 1. 目标

建立 Phase 3 所需的最小登录身份、Session、当前用户和 Owner 安全边界，同时保持 Collector Bearer Token 链不被破坏。

## 2. 前置依赖

- TASK-020 已完成。
- 必须阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 开始前执行 `git status`、`git log --oneline -8`、`git diff --check`。

## 3. 本任务范围

- 按 Accepted Identity MVP Contract 创建账号模型。
- 安全密码摘要。
- 登录、登出、当前用户接口。
- Session Cookie。
- CSRF 方案。
- 用户 timezone。
- 受控的初始账号 Bootstrap。
- 建立 AI endpoint 的认证基础。
- 根据 TASK-020 决策处理现有 Job Query API 的认证范围。
- 更新前后端测试基础设施，使后续任务可获得 authenticated user。

## 4. 不在本任务范围

- 不实现公共注册。
- 不实现 OAuth / MFA / 找回密码。
- 不实现 RBAC / tenant / organization。
- 不创建 Prompt 表。
- 不调用 AI。
- 不修改 Collector Bearer Token 语义。

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

- [ ] 明文密码永不落库/日志
- [ ] Login / Logout / Me 可用
- [ ] Session 安全属性符合约定
- [ ] 状态修改 API 有 CSRF 防护
- [ ] Collector 接入测试不回归
- [ ] 未认证 AI API 被拒绝
- [ ] Owner 身份可供后续 Service 使用
- [ ] 用户 timezone 可获取
- [ ] Java 测试通过
- [ ] Web/E2E 基线按认证决策通过
- [ ] CURRENT_STATUS 指向 TASK-022

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
