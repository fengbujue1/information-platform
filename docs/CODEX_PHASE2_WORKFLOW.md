# Phase 2 Codex 协作流程

## 1. 第一次启动 Phase 2

发送给 Codex：

```text
请恢复 Information Platform 的最新项目上下文，不要立即修改代码。

当前背景：

- Phase 1 已完成。
- Collector → Information Hub → MySQL → Job Query API 已打通。
- Phase 2 将建设标准化职位 Web 浏览 MVP。
- 当前 TASK 是 docs/tasks/TASK-011.md。
- TASK-011 只冻结范围和文档，不创建 Vue 代码。

请按顺序阅读：

1. 根目录 AGENTS.md
2. README.md
3. docs/PROJECT_CONTEXT.md
4. docs/ARCHITECTURE.md
5. docs/ROADMAP.md
6. docs/CURRENT_STATUS.md
7. docs/PHASE2_SCOPE.md
8. docs/contracts/job-query-api-v1.md
9. docs/contracts/web-ui-behavior-v1.md
10. docs/decisions/ADR-010-phase2-web-mvp-boundary.md
11. docs/tasks/TASK-011.md
12. frontend/information-hub-web/AGENTS.md
13. 最近 8 条 Git 提交

然后执行：

git status
git log --oneline -8
git diff --check

请汇报：

1. 当前项目目标。
2. Phase 1 已完成的真实能力。
3. Phase 2 推荐范围。
4. Phase 2 明确不做什么。
5. 当前文档是否有冲突。
6. TASK-011 准备修改哪些文件。
7. 需要我确认的决定。
8. 文档验证方法。

不要修改代码和文件，等待我确认。
```

## 2. 确认 TASK-011 的回复样例

```text
确认采用以下 Phase 2 决策：

1. Phase 2 只实现标准化职位 Web 浏览 MVP。
2. 使用现有 Job Query API V1。
3. 实现职位列表、筛选、排序、分页、详情和历史快照。
4. rawPayload 推迟到具备管理端认证以后。
5. 当前不建设用户系统和复杂权限。
6. 当前不增加职位写操作。
7. 当前不启动 AI、推荐和通知。
8. 开发环境使用 Vite Proxy。
9. 部署优先使用 Nginx 同源反向代理。
10. 未增加读取认证前，不允许公网无认证部署。
11. 快照先实现版本查看，不做复杂逐字 Diff。
12. TASK-011 不创建 Vue 代码。

请按 docs/tasks/TASK-011.md 实施。

完成后：

- 更新 TASK-011 实施记录；
- 更新测试结果；
- 将 Phase 2 范围、Web UI Behavior V1 和 ADR-010 改为 Accepted；
- 将 CURRENT_STATUS 当前任务改为 TASK-012；
- 不提交 Git。
```

## 3. 开始 TASK-012

```text
请实施 docs/tasks/TASK-012.md。

先阅读：

1. 根目录 AGENTS.md
2. docs/PHASE2_SCOPE.md
3. docs/contracts/web-ui-behavior-v1.md
4. docs/tasks/TASK-012.md
5. frontend/information-hub-web/AGENTS.md
6. 当前 Git 状态和最近 5 条提交

严格要求：

- 只创建 Vue 3 项目骨架；
- 使用 TypeScript、Vite、Vue Router、Element Plus、Pinia、Axios；
- 使用 npm 并提交 package-lock.json；
- 配置 Vitest 和 Vue Test Utils；
- 固定受维护的 Node LTS；
- 只创建 /jobs 和 404 占位页；
- 不调用真实 API；
- 不实现职位列表；
- 不修改 Java、Python 和数据库；
- 不提交 Git。

修改前先汇报：

1. 当前 Node 和 npm 版本。
2. 准备固定的 Node 版本。
3. 依赖列表。
4. 目录结构。
5. 创建和修改的文件。
6. npm scripts。
7. 测试方案。
8. 明确不实施的内容。

等待我确认后再修改。
```

## 4. 通用 TASK 开始模板

```text
请实施 docs/tasks/TASK-XXX.md。

开始前必须阅读：

1. 根目录 AGENTS.md
2. docs/PHASE2_SCOPE.md
3. docs/contracts/job-query-api-v1.md
4. docs/contracts/web-ui-behavior-v1.md
5. docs/CURRENT_STATUS.md
6. 当前 TASK
7. frontend/information-hub-web/AGENTS.md
8. 当前任务涉及的现有代码
9. 最近 5 条 Git 提交

先执行：

git status
git diff --check

不要立即修改。

先汇报：

1. 当前 TASK 的目标。
2. 前置依赖是否满足。
3. 计划修改文件。
4. 组件和模块拆分。
5. 数据流。
6. 测试方案。
7. 风险和待确认点。
8. 明确不在范围的内容。

等待我确认后再实施。

实施时必须：

- 只完成当前 TASK；
- 不增加 rawPayload；
- 不增加用户系统；
- 不增加写操作；
- 不修改 Collector；
- 不提前进入 AI、推荐和通知；
- 不把秘密写入前端；
- 完成后更新 TASK 和 CURRENT_STATUS；
- 如果涉及新增文件，需要使用 git add进行git追踪；
- 不提交 Git。
```

## 5. Codex 发现 API 契约不一致时

```text
先停止实现，不要自行修改前端类型或后端 API 来掩盖问题。

请给出：

1. Job Query API V1 的定义。
2. 后端实际 DTO 和 Controller 的行为。
3. 前端当前需要的字段。
4. 具体不一致位置。
5. 是否属于 Bug、兼容扩展或范围变更。
6. 最小修复方案。
7. 需要修改的合同、代码和测试。
8. 对当前 TASK 的影响。

等待我决定是：

- 在当前 TASK 内修复明确 Bug；
- 新增子任务；
- 修改协议版本；
- 暂时使用现有字段。
```

## 6. 遇到需求膨胀时

```text
这是一次范围变更，不要直接塞进当前 TASK。

当前 TASK：
TASK-XXX，原目标是……

新增需求：
……

新增原因：
……

可能影响：
……

请先判断：

1. 是否属于当前 TASK。
2. 是否需要新 TASK。
3. 是否需要 ADR。
4. 是否需要修改 Phase 2 范围。
5. 是否会引入认证、数据库、部署或安全风险。
6. 是否会导致提前进入 Phase 3。

本轮只做评估，不修改代码。

请输出：

- 范围判断；
- 推荐任务编号和名称；
- 前置和后续依赖；
- 修改文件；
- 验收标准；
- 不在范围的内容；
- 风险；
- 需要我确认的决定。
```

## 7. 审查当前未提交修改

```text
请审查当前未提交修改，不要立即重写。

检查：

1. 是否符合当前 TASK。
2. 是否违反根 AGENTS 和前端 AGENTS。
3. 是否出现 any 绕过 API 类型。
4. 是否在页面中直接创建 Axios。
5. 是否把 URL 状态和页面状态做成两套相互冲突的来源。
6. 是否请求或展示 rawPayload。
7. 是否保存了 Collector Token、数据库密码或服务器秘密。
8. 是否使用 v-html。
9. 外部链接是否安全。
10. 是否正确处理 nullable 字段。
11. 是否处理 Loading、Empty、Error 和 404。
12. 是否存在请求竞态或重复请求。
13. 测试是否覆盖异常场景。
14. 文档是否与代码一致。
15. 是否提前实现后续 TASK。

按严重程度输出：

- 阻塞问题；
- 高风险问题；
- 一般问题；
- 最小修复建议；
- 建议补充测试。

不要提交 Git。
```

## 8. 每个 TASK 完成前

```text
本轮 TASK 准备收尾，请进行交接检查。

请完成：

1. git status
2. git diff --check
3. 运行当前 TASK 要求的 typecheck、test 和 build
4. 检查是否超出 TASK
5. 检查是否包含秘密
6. 更新当前 TASK 的实施记录
7. 写入实际测试命令和结果
8. 更新 docs/CURRENT_STATUS.md
9. 明确下一步最小 TASK
10. 不提交 Git

最后汇报：

- 修改文件；
- 完成功能；
- 测试命令和结果；
- 未完成项；
- 风险；
- 下一台电脑从哪里继续。
```

## 9. 用户检查并提交

```powershell
git status
git diff --check
git diff
```

前端任务通常还应执行：

```powershell
Set-Location frontend/information-hub-web
npm ci
npm run typecheck
npm run test
npm run build
```

确认后：

```powershell
git add .
git commit -m "feat(web): complete task-xxx"
git push
```

## 10. 两台电脑切换

离开当前电脑前：

```powershell
git status
git diff
git add .
git commit -m "wip(web): checkpoint task-xxx"
git push
```

另一台电脑：

```powershell
git fetch --all
git switch dev
git pull --rebase
git status
```

然后使用本文件第 1 节的恢复指令，并把当前 TASK 编号替换为实际任务。
