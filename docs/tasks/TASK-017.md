# TASK-017：完善交互、响应式和健壮性

状态：DONE
所属阶段：Phase 2
优先级：P1
负责人：User + Codex

## 1. 目标

统一 Phase 2 页面体验，修复在窄屏、异常数据和网络异常下的问题。

## 2. 背景

TASK-014 至 TASK-016 完成核心页面。本任务只做跨页面一致性和质量收口，不新增业务模块。

## 3. 前置依赖

- TASK-014 至 TASK-016 已完成。

## 4. 影响范围

- 通用 Layout
- Loading、Empty、Error、404 组件
- 日期和薪资格式化
- 响应式布局
- 可访问性
- 现有页面测试
- TASK 和 CURRENT_STATUS

## 5. 本任务范围

- 统一页面布局和导航。
- 抽取通用状态组件。
- 处理超长标题、JD、标签和 JSON。
- 完善桌面和窄屏布局。
- 完善键盘操作和基础 ARIA。
- 统一日期、空值、薪资和错误提示。
- 处理重复点击和请求竞态。
- 检查浏览器前进后退。
- 检查直接访问深层路由。
- 增加必要回归测试。

## 6. 不在本任务范围

- 不重新设计品牌视觉系统。
- 不增加暗色模式，除非用户单独确认。
- 不增加国际化。
- 不增加用户偏好数据库。
- 不增加新后端接口。
- 不新增 Phase 3 功能。

## 7. 业务与技术规则

- 修复优先于重构。
- 不为了复用创建过度抽象。
- 页面主要功能必须可用键盘操作。
- 颜色不能是唯一状态表达方式。
- 小屏不能依赖横向无限滚动完成核心浏览。

## 8. 验收标准

- [x] 三个核心页面视觉和状态一致
- [x] 常见窄屏可用
- [x] 长文本不破坏布局
- [x] 错误信息可理解
- [x] 请求竞态不会覆盖最新结果
- [x] 基础可访问性检查通过
- [x] typecheck、test、build 通过
- [x] CURRENT_STATUS 更新为 TASK-018

## 9. 实施前计划

Codex 先进行问题清单审查，不得先大规模重写。

实施前确认采用以下最小方案：

- 增加统一页面壳、主要导航和跳转正文入口。
- 使用一个轻量 `PageState` 统一 Loading、Empty、Error、Not Found 和非法地址状态。
- 保留现有 API、URL 状态、请求取消和乱序保护。
- 桌面保留完整分页，窄屏使用紧凑分页。
- 通过 CSS 约束长文本、详情表格、标签、Hash 和 JSON，不重写业务组件。

## 10. 实施记录

实施日期：2026-07-30

- 更新应用页面壳，增加返回职位入口、主要导航、跳转正文链接和全局键盘焦点样式。
- 新增通用 `PageState`，统一列表、详情、快照和 404 的 Loading、Empty、Error、Not Found 与非法地址状态。
- 错误状态使用 `role="alert"`，普通状态使用 `role="status"`；Loading 增加 `aria-busy` 和可读提示。
- 列表页增加桌面完整分页和窄屏紧凑分页，小屏不再依赖横向滚动完成翻页。
- 为 Grid 子项、职位卡片、标签、详情字段、快照 Hash 和标准化 JSON 增加长内容约束。
- 快照版本列表改用语义化列表，保留原生按钮和 `aria-pressed`，并增加当前项标识。
- 保留 `useJobList`、`useJobDetail`、`useJobSnapshots` 的请求取消、重试保护和乱序响应保护，不修改 API 数据流。
- 增加通用状态、应用页面壳、长内容卡片、紧凑分页和状态事件回归测试。
- 未增加 rawPayload、写操作、用户系统、后端接口、Collector 修改或 Phase 3 功能。

修改文件：

- `frontend/information-hub-web/src/App.vue`
- `frontend/information-hub-web/src/main.ts`
- `frontend/information-hub-web/src/components/common/PageState.vue`
- `frontend/information-hub-web/src/components/common/PageState.spec.ts`
- `frontend/information-hub-web/src/components/jobs/JobListResults.vue`
- `frontend/information-hub-web/src/components/jobs/JobListResults.spec.ts`
- `frontend/information-hub-web/src/components/jobs/JobSnapshotList.vue`
- `frontend/information-hub-web/src/views/JobDetailView.vue`
- `frontend/information-hub-web/src/views/JobSnapshotsView.vue`
- `frontend/information-hub-web/src/views/NotFoundView.vue`
- `frontend/information-hub-web/src/styles/task017.css`
- `frontend/information-hub-web/src/styles/task017-overflow.css`
- `frontend/information-hub-web/src/tests/appShell.spec.ts`
- `docs/tasks/TASK-017.md`
- `docs/CURRENT_STATUS.md`

## 11. 测试结果

在 `frontend/information-hub-web` 执行：

```text
npm.cmd run typecheck
```

结果：通过。

```text
npm.cmd run test
```

结果：通过，14 个测试文件、62 个测试全部成功。

```text
npm.cmd run build
```

结果：通过，Vite 生产构建成功。

浏览器响应式检查：

- 使用 375×812 视口检查职位列表、职位详情、历史快照和 404。
- 列表、详情、快照和 404 均无页面级水平溢出。
- 窄屏只显示紧凑分页，桌面完整分页被隐藏。
- 长职位正文、长标签、长字段和标准化 JSON 未撑破页面。
- 快照当前版本具有 `aria-current`，页面壳具有可用的跳转正文入口。

Git 检查：

```text
git diff --check
```

结果：通过。

## 12. 遗留问题

- TASK-019 仍需使用真实 Information Hub 完成 Phase 2 端到端验收。
- 本任务只做常见窄屏基础适配，不覆盖所有浏览器和真实设备组合。

## 13. 完成确认

- [x] CURRENT_STATUS 已更新
- [ ] 用户已检查 git diff
- [ ] 用户已确认测试结果
- [ ] 已提交并 push
