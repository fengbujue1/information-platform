# TASK-022：实现 Prompt Profile 与 Prompt Version

状态：DONE
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

- [x] 一个用户可有多个 Profile
- [x] 修改 Prompt 产生新 Version
- [x] 旧 Version 内容不可修改
- [x] Active Version 正确切换
- [x] contentHash 稳定
- [x] 用户 A 不能访问用户 B Prompt
- [x] Prompt 长度限制生效
- [x] 不存在 `user_account.prompt` 单字段捷径
- [x] 测试通过
- [x] CURRENT_STATUS 指向 TASK-023

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

- 新增 `analysis.prompt` Domain、Application 和 API 分层，复用 TASK-024 已落地的 `ai_prompt_profile` / `ai_prompt_version` PO 与表结构。
- 实现 Contract 规定的 7 个 `/api/v1/ai/prompt-profiles` 接口，Owner 只来自当前 Session。
- Profile 支持账号内多配置和 `ACTIVE` / `DISABLED` 状态，不提供物理删除。
- Version 内容不可变；同一 Profile 内按原文 UTF-8 SHA-256 复用相同 `contentHash`，不同内容使用递增 `versionNo`。
- 创建或复用 Version 与 Active Version 切换处于同一事务；通过 Profile 行锁串行化同一 Profile 的版本号分配。
- Profile 状态使用 Owner 限定的固定 SQL 更新，仅修改状态列并由数据库维护 `updated_at`。
- Prompt 限制为 8000 个 Unicode 字符，首版只接受 `JOB_USER_RELEVANCE`。
- 前端仅增加 Prompt API 类型、同源 Session/CSRF 调用封装和 API 单元测试，未增加管理页面。
- 未修改 Flyway Migration、Accepted ADR、Contracts 或数据库设计。

## 9. 测试结果

- `.\mvnw.cmd test`（最终审查前、SSH 测试库可用）：79 个测试，78 通过，0 失败，1 跳过；TASK-022 真实 MySQL 集成测试通过，仅独立空库 Migration 测试跳过。
- `.\mvnw.cmd -Dtest=PromptContentHasherTest,PromptProfileServiceTest,PromptProfileControllerTest test`（最终审查修复后）：16 个测试通过，0 失败，0 跳过。
- `.\mvnw.cmd test`（最终审查修复后、清除可选数据库变量）：83 个测试，64 通过，0 失败，19 个数据库条件测试跳过。
- `.\mvnw.cmd -DskipTests package`：后端可执行 JAR 打包通过。
- `npm test`：17 个测试文件、67 个测试全部通过。
- `npm run build`：TypeScript 类型检查与 Vite 生产构建通过。

## 10. 遗留问题

- MySQL 8.4 仍会显示当前 Flyway 版本最新测试到 MySQL 8.1 的既有升级提示，不影响本任务测试结果。
- 最终审查修复 `updated_at` 更新语义后，本机 SSH 测试库端口不可达，新增的真实库时间戳断言尚未重跑；固定 SQL 已通过编译、单元测试和无数据库完整回归。
- 完整 Prompt 管理 UI、Analysis Definition、Provider 和 Analysis Engine 按后续 TASK 实施。

## 11. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [ ] 用户已检查 `git diff`
- [ ] 用户确认测试结果
- [ ] 用户完成 commit / push
