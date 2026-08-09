# TASK-043：Phase 4 Web — Profile、Feed、Refresh、Feedback 与 BOSS Contact Status

状态：DONE

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

完成 Phase 4 最小可用 Web，让用户配置画像、查看推荐、刷新、反馈并维护“已联系/已联系且不合适”。

## 2. 前置依赖

TASK-035、036、040、042。

## 3. 实施范围

- Recommendation page；
- Profile form；
- Feed card/list；
- score/reasons；
- open source/BOSS link；
- Manual Refresh；
- Run polling；
- Interested；
- Not Interested；
- Contacted；
- Contacted Not Suitable；
- Viewed；
- CONTACTED badge；
- hard exclusion card immediate removal；
- undo/reset action；
- profile stale hint；
- error states。

## 4. 明确不做

- 自动读取 BOSS 聊天；
- 全站 redesign；
- complex animations；
- Notification；
- Product Polish。

## 5. 验收与测试

profile save、refresh/polling、feed、feedback、contact disposition、immediate hide、CONTACTED remains、undo, stale hint, error handling, browser E2E。

## 6. 文档同步

- frontend docs as needed
- CURRENT_STATUS

## 7. 完成前统一检查

- 当前 TASK 测试通过；
- `git diff --check`；
- 未超 Scope；
- 后端改动按 `docs/LOGGING_CONVENTIONS.md` 检查；
- 更新当前 TASK 实施记录；
- 更新 `docs/CURRENT_STATUS.md`；
- 如数据库/架构事实改变，同步事实文档；
- 汇报实际命令与结果。

## 8. 实施记录

完成日期：2026-08-09

已实施：

- 新增受现有 Session 路由守卫保护的 `/recommendations` 页面及“职位推荐”主导航；
- 新增严格类型化的 Recommendation Profile、Feed、Run、Interaction API Client，所有 Profile、Refresh、View、Feedback 与 JOB disposition 写请求复用同源 CSRF；
- Profile 表单支持绑定同 Owner JOB_USER_RELEVANCE Prompt Profile、窗口、Top N、岗位、技能、城市、办公方式、最低月薪和排除关键词；保存只持久化 Profile，不自动触发 Analysis 或 Refresh；
- Feed 展示预计算 final score、AI/Profile/Freshness score breakdown、reasons、JOB 摘要、安全来源/BOSS 链接、Viewed、Interested 与 CONTACTED 状态；
- Manual Refresh 创建 PENDING Run，页面轮询 Run detail 到 COMPLETED、NOOP 或 FAILED；终态后刷新 Run 摘要与 Feed，FAILED 保留旧成功 Feed；
- Generic Feedback 与 JOB disposition 使用独立写操作；CONTACTED 保留卡片并显示“已联系”，NOT_INTERESTED 与 CONTACTED_NOT_SUITABLE 成功写入后立即移除当前卡片；
- hard exclusion 操作提供显式撤销，将对应状态恢复为 NONE 后重新读取 Feed；
- Profile hash 变化时展示 stale 提示，不自动刷新；覆盖初始加载、操作、轮询和 Feed 失败错误状态；
- 增加 API、卡片、页面、路由单元/组件测试，以及使用合成 API Fixture 的 Playwright 浏览器验收；未实施 TASK-044 的真实后端、MySQL、Fake Provider 全栈 E2E；
- 未修改后端业务代码、数据库或 Flyway，未实现 Notification、BOSS 聊天自动读取或全站 redesign。

验证：

- 当前 shell：Node.js 22.14.0、npm 10.9.2；本机未安装仓库 `.nvmrc` 固定的 Node.js 24.18.0；
- `npm.cmd run typecheck`：通过；
- `npm.cmd run test`：28 个测试文件、87 tests，全部通过；
- `npm.cmd run test:e2e`：Chromium 8 tests，全部通过；Recommendation 浏览器用例覆盖 Profile save、Feed、Viewed、stale、Feedback、CONTACTED、hard exclusion、undo、Refresh polling 与错误态；
- `npm.cmd run build`：类型检查与 Vite 生产构建成功。
