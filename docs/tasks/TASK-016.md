# TASK-016：实现历史快照查看

状态：DONE
所属阶段：Phase 2
优先级：P0
负责人：User + Codex

## 1. 目标

实现职位历史版本列表和单个版本内容查看。

## 2. 背景

Phase 1 已保存不可变历史快照。Phase 2 需要把这些版本转化为可浏览界面。

## 3. 前置依赖

- TASK-015 已完成。

## 4. 影响范围

- `/jobs/:id/snapshots`
- 快照列表组件
- 快照详情组件
- JSON 只读展示
- 测试
- TASK 和 CURRENT_STATUS

## 5. 本任务范围

展示：

- 快照版本号；
- 快照创建时间；
- 采集时间；
- 标题；
- 正文；
- contentHash；
- standardizedPayload；
- collectorVersion。

实现：

- 快照 Loading；
- 无历史版本；
- 请求错误；
- 选择版本；
- 从快照返回当前详情；
- 直接访问快照路由。

## 6. 不在本任务范围

- 不查看 snapshot rawPayload。
- 不实现逐字 Diff。
- 不修改快照。
- 不删除快照。
- 不重新执行 AI。
- 不新增数据库字段。

## 7. 业务与技术规则

- 使用后端既定排序。
- standardizedPayload 只读格式化展示。
- JSON 展示必须处理超长内容。
- 快照为空不是错误。
- contentHash 可放在次要信息区。
- 页面不能假设版本号连续无缺口。

## 8. 验收标准

- [x] 多版本正常显示
- [x] 无快照状态正确
- [x] 选中版本可查看内容
- [x] 直接刷新路由可用
- [x] standardizedPayload 可读
- [x] 不返回或显示 rawPayload
- [x] typecheck、test、build 通过
- [x] CURRENT_STATUS 更新为 TASK-017

## 9. 实施前计划

- 使用独立 View、快照列表、快照内容和请求组合逻辑，页面不直接操作 Axios。
- 保留后端 `createdAt DESC, versionNo DESC` 顺序，默认选择第一项，不假设版本号连续。
- standardizedPayload 使用 `JSON.stringify(value, null, 2)` 和只读 `<pre>` 展示，不执行 JSON 内文本。
- 只格式化当前选中版本，通过滚动容器控制超长 JSON 和正文的布局影响。
- 覆盖多版本、空列表、Loading、404、网络重试、非法 ID、返回导航、请求取消和迟到响应。

## 10. 实施记录

完成时间：2026-07-30

- 将 `/jobs/:id/snapshots` 占位页替换为真实历史快照页面。
- 使用 `useJobSnapshots` 集中处理 ID 校验、加载、请求取消、乱序响应、404、重试和选中版本。
- 快照列表严格保留后端顺序，以快照 ID 作为选择标识，支持非连续版本号。
- 默认展示第一条快照，切换版本时同步更新标题、正文、时间、Hash、Collector 和标准化业务 JSON。
- standardizedPayload 使用只读格式化文本展示；HTML 类字符串保持惰性文本，不使用 `v-html`。
- 空快照显示正常 Empty 状态，不作为请求错误。
- 返回职位详情和职位列表时继续携带经过校验的 `from`。
- 未新增依赖，未修改 Java、Python、Collector、数据库或 API 契约。

## 11. 测试结果

- `npm.cmd run typecheck`：通过。
- `npm.cmd run test`：通过，11 个测试文件、53 项测试全部通过。
- `npm.cmd run build`：通过，Vite 生产构建成功，转换 1711 个模块。
- 构建主要产物：CSS 约 109.67 kB（gzip 15.83 kB），JS 约 462.01 kB（gzip 159.38 kB）。
- 测试覆盖后端顺序、非连续版本、版本切换、JSON 安全文本、空列表、Loading、404、网络重试、非法 ID、导航恢复、请求取消和迟到响应。

## 12. 遗留问题

- 当前快照 API 一次返回全部版本；出现真实规模瓶颈后再独立评估分页，不在本任务扩展后端。
- 当前版本选择是页面局部状态，刷新后按后端顺序重新选择第一项；本任务不新增版本 URL 参数。
- TASK-017 统一完善三个核心页面的交互、窄屏、长文本和基础可访问性。
- 真实前后端端到端验收仍由 TASK-019 完成。

## 13. 完成确认

- [x] CURRENT_STATUS 已更新
- [ ] 用户已检查 git diff
- [ ] 用户已确认测试结果
- [x] 新增文件已加入 Git 追踪
- [x] 未提交 Git，等待用户检查
