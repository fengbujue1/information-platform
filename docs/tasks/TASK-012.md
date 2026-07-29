# TASK-012：创建 Vue 3 项目骨架

状态：DONE
所属阶段：Phase 2  
优先级：P0  
负责人：User + Codex

## 1. 目标

在 `frontend/information-hub-web` 中创建可安装、启动、测试、类型检查和构建的 Vue 3 项目骨架。

## 2. 背景

前端目录此前只有 AGENTS，没有实际 Vue 项目。

本任务只建立工程基础，不实现职位列表和详情业务。

## 3. 前置依赖

- TASK-011 已完成。
- Phase 2 范围和 Web UI Behavior V1 已 Accepted。

## 4. 影响范围

- `frontend/information-hub-web/`
- `docs/tasks/TASK-012.md`
- `docs/CURRENT_STATUS.md`

## 5. 本任务范围

- 检查本机 Node 和 npm 环境。
- 选择并固定一个受维护的 Node LTS。
- 使用 npm。
- 创建 Vue 3 + TypeScript + Vite。
- 安装 Vue Router、Element Plus、Pinia 和 Axios。
- 配置 Vitest、Vue Test Utils 和 jsdom。
- 创建最小应用壳。
- 创建 `/jobs` 占位路由。
- 创建 404 占位路由。
- 创建 `.env.example`。
- 创建必要的 npm scripts。
- 增加基础启动和渲染测试。
- 保留并遵守现有 AGENTS。

## 6. 不在本任务范围

- 不调用真实 API。
- 不实现职位筛选。
- 不实现职位详情。
- 不实现快照。
- 不创建后端代码。
- 不配置公网部署。
- 不增加认证。
- 不引入 UI 框架之外的大型依赖。

## 7. 业务与技术规则

- 提交 `package-lock.json`。
- 不提交 `node_modules`。
- Node 版本通过 `.nvmrc`、`.node-version` 或等效文件固定。
- 不在前端环境变量中保存秘密。
- 应用默认入口为 `/jobs`。
- 项目必须可从 Windows 环境运行。

## 8. 验收标准

- [x] `npm ci` 成功
- [x] `npm run dev` 可启动
- [x] `npm run typecheck` 通过
- [x] `npm run test` 通过
- [x] `npm run build` 通过
- [x] `/jobs` 占位页可访问
- [x] 未实现后续业务功能
- [x] CURRENT_STATUS 当前任务更新为 TASK-013

## 9. 实施前计划

Codex 必须先列出：

- 选择的 Node 版本和理由
- 依赖列表
- 目录结构
- 创建文件
- 测试方案
- 不在本任务中的内容

已在实施前完成汇报，并经用户确认后开始修改。

## 10. 实施记录

- 实施前本机为 Node.js `22.23.1`、npm `10.9.8`。
- 安装并固定 Node.js `24.18.0` LTS 与 npm `11.16.0`。
- 使用 `.nvmrc`、`package.json` 的 `engines` 和 `packageManager` 记录运行环境。
- 创建 Vue 3、TypeScript、Vite 和 Vue Router 项目骨架。
- 安装 Element Plus、Pinia 和 Axios；Pinia 仅在应用入口注册，Axios 未被调用。
- 配置 Vitest、Vue Test Utils 和 jsdom。
- 创建最小应用壳、`/jobs` 占位页和 404 占位页；根路径重定向到 `/jobs`。
- Element Plus 使用组件级引入，避免骨架阶段全量打包 UI 库。
- 创建 `.env.example`，仅声明可公开的应用标题，不包含密码、Token 或 Cookie。
- 创建并保留 `package-lock.json`，未纳入 `node_modules` 和 `dist`。
- TypeScript 固定为 `6.0.3`，避免当前 TypeScript 7 与 `vue-tsc` 的导出入口不兼容。
- Vue Test Utils 固定为 `2.2.7`，避开较新版本经 `js-beautify` 引入的已知高危依赖链。
- 未创建 API Client、业务 Store、职位列表、详情或快照功能。
- 未修改 Java、Python、数据库或 BOSS Collector。

## 11. 测试结果

执行环境：

- `node --version`：`v24.18.0`
- `npm --version`：`11.16.0`

验证结果：

- `npm ci`：通过，按最终锁文件安装 202 个包。
- `npm run typecheck`：通过。
- `npm run test`：通过，1 个测试文件、3 个测试全部通过。
- `npm run build`：通过；主要 JS 产物约 131.84 kB，gzip 后约 49.75 kB。
- `npm run dev -- --host 127.0.0.1 --port 4173 --strictPort`：开发服务器可启动。
- 开发服务器冒烟验证：`/jobs` 与未知路径均返回 SPA 入口；单元测试确认分别渲染职位占位页和 404 占位页。
- `npm audit --audit-level=high`：通过，0 个已知漏洞。

在当前 Windows PowerShell 执行策略下，直接输入 `npm` 会优先命中受限的 `npm.ps1`；验证时使用等价的 `npm.cmd` 执行 npm scripts。

## 12. 遗留问题

- TASK-013 再实现 API Base URL、Vite Proxy、统一 Axios Client、接口类型和格式化函数。
- 当前没有职位列表、详情和快照业务页面，符合本任务边界。
- 部署环境的 SPA fallback 和 Nginx 同源代理留给后续部署任务。
- 如 PowerShell 禁止执行 `npm.ps1`，可直接使用 `npm.cmd`；无需为本项目放宽系统执行策略。

## 13. 完成确认

- [x] CURRENT_STATUS 已更新
- [ ] 用户已检查 git diff
- [ ] 用户已确认测试结果
- [x] 未提交 Git，等待用户检查
