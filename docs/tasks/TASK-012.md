# TASK-012：创建 Vue 3 项目骨架

状态：TODO  
所属阶段：Phase 2  
优先级：P0  
负责人：User + Codex

## 1. 目标

在 `frontend/information-hub-web` 中创建可安装、启动、测试、类型检查和构建的 Vue 3 项目骨架。

## 2. 背景

前端目录目前只有 AGENTS，没有实际 Vue 项目。

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

- [ ] `npm ci` 成功
- [ ] `npm run dev` 可启动
- [ ] `npm run typecheck` 通过
- [ ] `npm run test` 通过
- [ ] `npm run build` 通过
- [ ] `/jobs` 占位页可访问
- [ ] 未实现后续业务功能
- [ ] CURRENT_STATUS 当前任务更新为 TASK-013

## 9. 实施前计划

Codex 必须先列出：

- 选择的 Node 版本和理由
- 依赖列表
- 目录结构
- 创建文件
- 测试方案
- 不在本任务中的内容

## 10. 实施记录

待填写。

## 11. 测试结果

待填写实际命令和结果。

## 12. 遗留问题

待填写。

## 13. 完成确认

- [ ] CURRENT_STATUS 已更新
- [ ] 用户已检查 git diff
- [ ] 用户已确认测试结果
- [ ] 已提交并 push
