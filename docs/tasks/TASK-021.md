# TASK-021：实现 Identity MVP

状态：DONE
所属阶段：Phase 3
优先级：P0
负责人：User + Codex

## 1. 目标

建立 Phase 3 所需的最小登录身份、Session、当前用户和 Owner 安全边界，同时保持 Collector Bearer Token 链不被破坏。

## 2. 前置依赖

- TASK-024 已完成，`user_account` 及相关 Phase 3 表已迁移。
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

- [x] 明文密码永不落库/日志
- [x] Login / Logout / Me 可用
- [x] Session 安全属性符合约定
- [x] 状态修改 API 有 CSRF 防护
- [x] Collector 接入测试不回归
- [x] 未认证 AI API 被拒绝
- [x] Owner 身份可供后续 Service 使用
- [x] 用户 timezone 可获取
- [x] Java 测试通过
- [x] Web/E2E 基线按认证决策通过
- [x] CURRENT_STATUS 指向 TASK-022

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

- 后端新增 `identity` 模块的登录、当前用户、受控 Bootstrap、Session/CSRF 安全配置和后续 Service 可复用的 `CurrentUserProvider`。
- 使用 Spring Security `DelegatingPasswordEncoder` 保存密码摘要；Bootstrap 密码仅从环境配置读取，不提供默认密码，不写入日志或响应。
- Bootstrap 在凭据缺失时不进入事务或访问数据库，并将并发唯一键冲突视为已由另一实例完成创建。
- `/api/v1/jobs/**` 与 `/api/v1/ai/**` 纳入 Session 认证；`/api/v1/collector/**` 继续由既有 Bearer Token 过滤器独立保护。
- Session Cookie 通过 Spring Boot `server.servlet.session.cookie` 正确绑定 `HttpOnly`、`SameSite`、`Secure` 和 Path；登录成功后执行 Session ID 轮换，登出要求 CSRF 并使 Session 失效。
- 前端新增登录页、当前用户会话恢复、安全 redirect、CSRF token 获取与缓存、全局 401 清理和登出入口。
- 现有 Job Web 与 E2E 测试夹具已适配认证前置条件；未实现任何 Prompt、Provider、Analysis、Batch 或 Schedule 业务。

## 9. 测试结果

- 后端：清除不安全的 `INFORMATION_HUB_TEST_DB_*` 与 `INFORMATION_HUB_EMPTY_TEST_DB_*` 环境覆盖后执行 `.\mvnw.cmd test`，结果为 66 tests、0 failures、0 errors、18 skipped，构建成功。
- 前端单元测试：`npm test`，16 个测试文件、65 个测试全部通过。
- 前端类型检查与生产构建：`npm run build`，`vue-tsc --noEmit` 与 Vite build 均通过。
- 前端浏览器测试：`npm run test:e2e`，Chromium 6 个场景全部通过，包含登录恢复、Session 失效 401 跳转和 CSRF 登出。

## 10. 遗留问题

- 当前机器原有数据库测试环境变量指向名称包含 `dev` 的库，测试安全校验按设计拒绝执行；因此 18 个条件数据库集成测试未运行。本任务未绕过该保护，也未修改 Migration。
- Bootstrap 账号必须由部署环境显式提供用户名、密码、显示名和时区；仓库不包含真实凭据。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
