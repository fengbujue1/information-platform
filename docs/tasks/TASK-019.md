# TASK-019：完成 Phase 2 端到端验收

状态：DONE
所属阶段：Phase 2
优先级：P0
负责人：User + Codex

## 1. 目标

验证浏览器、前端、Job Query API、Information Hub 和 MySQL 的完整读取链路，并完成 Phase 2 文档收尾。

## 2. 背景

单元测试和组件测试不能替代真实的浏览器与后端联调。

## 3. 前置依赖

- TASK-011 至 TASK-018 全部完成。
- 开发数据库中存在可浏览的职位和至少一个多版本职位。

## 4. 影响范围

- 前端端到端测试
- 验收记录
- README
- ROADMAP
- CURRENT_STATUS
- TASK-019

## 5. 本任务范围

自动化稳定场景：

- 列表加载；
- 筛选；
- 排序；
- 分页；
- 列表进入详情；
- 详情进入快照；
- 404；
- API 错误状态；
- 空数据状态；
- 深层路由刷新。

真实环境人工联调：

```text
浏览器
→ Information Hub Web
→ Job Query API
→ Information Hub
→ MySQL
```

检查：

- SSH 隧道；
- 后端配置；
- 前端代理；
- 数据真实显示；
- 来源链接；
- UTC 到本地时间显示；
- 无 rawPayload 泄露；
- 无 Token 泄露。

## 6. 不在本任务范围

- 不新增功能。
- 不进入 Phase 3。
- 不公开部署。
- 不增加用户系统。
- 不为验收修改协议语义。
- 发现缺陷时只做最小修复。

## 7. 业务与技术规则

- 自动化 E2E 使用稳定 Fixture 和请求拦截，不依赖远程数据库数据顺序。
- 真实联调单独记录实际结果，不提交真实职位数据。
- 不把真实职位原始数据提交 Git。
- 验收失败不能把 Phase 2 标记完成。

## 8. 验收标准

- [x] npm typecheck 通过
- [x] npm test 通过
- [x] npm build 通过
- [x] 自动化浏览器烟雾测试通过
- [x] 真实后端联调通过
- [x] 列表、详情、快照主链路通过
- [x] 深层路由刷新通过
- [x] API 失败和空数据状态通过
- [x] 不返回或展示 rawPayload
- [x] 不泄露秘密
- [x] README 更新
- [x] ROADMAP Phase 2 标记完成
- [x] CURRENT_STATUS 更新为等待 Phase 3 规划
- [x] TASK-019 包含实际测试命令和结果

## 9. 实施前计划

- 使用固定版本 Playwright 和 Chromium。
- 使用仓库内合成 Fixture 拦截 `/api/v1/jobs`、详情和快照请求。
- 由测试入口在 `127.0.0.1:4173` 启动并关闭 Vite，不依赖外部常驻进程。
- 自动化测试不访问远程 MySQL、不读取真实职位、不要求 SSH 隧道。
- 失败时保留测试诊断产物，不修改真实数据库；修复仅限测试或真实发现的最小缺陷。
- 更新根 README、前端 README、ROADMAP、CURRENT_STATUS 和 TASK-019。

## 10. 实施记录

- 固定引入 `@playwright/test` 1.61.1，并提交 package-lock.json。
- 新增 Playwright Chromium 配置、跨平台 Vite 测试入口、标准职位 Fixture 和三个浏览器 E2E 场景。
- Fixture 只包含合成标准化字段，不包含真实职位原始数据、rawPayload、Token、Cookie、密码或服务器地址。
- E2E 覆盖列表加载、筛选、排序、分页、详情、来源链接安全属性、快照切换、深层路由刷新、空数据、API 错误和 404。
- E2E 明确断言页面不展示 `rawPayload` 和 `Collector Token`。
- Vitest 限定只收集 `src/**/*.spec.ts`，与 Playwright E2E 测试目录相互隔离。
- CI 前端 job 增加 Chromium 安装和 `npm run test:e2e`。
- 使用实际本地浏览器只读核对页面壳、筛选表单可访问名称和错误状态，临时 Vite 进程已关闭。
- 项目负责人确认真实环境人工端到端部分已完成，各功能目前没有发现遗漏。
- 更新 README、ROADMAP 和 CURRENT_STATUS，Phase 2 标记完成并等待 Phase 3 规划。
- 未修改 Java、Collector、数据库、API 契约或现有前端业务功能。

## 11. 测试结果

- `npm.cmd ci --no-audit --no-fund`：通过，按 lockfile 安装 208 个包，审计前一次安装报告 0 个漏洞。
- `npm.cmd run typecheck`：通过。
- `npm.cmd run test`：通过，14 个测试文件、62 个测试。
- `npm.cmd run test:e2e -- --workers=1`：通过，3 个 Chromium E2E 场景。
- `npm.cmd run build`：通过，Vite 生产构建成功。
- 本地浏览器 DOM 核对：页面壳、筛选表单可访问名称和 API 错误状态符合测试定位依据。
- 真实环境人工联调：由项目负责人完成并确认通过；覆盖浏览器、Information Hub Web、Job Query API、Information Hub 和 MySQL 的真实只读链路。
- `git diff --check` 与 `git diff --cached --check`：通过。
- 敏感文件与私钥标记检查：通过。

## 12. 遗留问题

- GitHub Actions 只有在本次修改提交并 push 后才会执行新增的 Playwright job。
- Phase 3 尚未设计冻结；不得直接开始 AI、推荐或通知实现。

## 13. 完成确认

- [x] CURRENT_STATUS 已更新
- [ ] 用户已检查 git diff
- [x] 用户已确认真实环境人工测试结果
- [x] Phase 2 已标记完成
- [ ] 已提交并 push
