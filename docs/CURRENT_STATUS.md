# 当前开发状态

更新时间：2026-07-30
当前分支：dev

## 当前阶段

Phase 1 已完成。

Phase 2 范围已冻结，Vue 3 项目骨架、强类型 Job Query API Client、标准化职位列表页、职位详情页和历史快照页已完成。

## Phase 1 已完成

- 创建 Monorepo 和模块级 AGENTS。
- 导入并验证 BOSS Collector。
- 完成真实 BOSS 列表和详情字段审计。
- 冻结 InformationEnvelope V1。
- 完成 Spring Boot Information Hub。
- 建立远程 Docker MySQL 开发与集成测试环境。
- 通过 Flyway 创建三张 Phase 1 数据表。
- 实现幂等接入、非破坏性合并和历史快照。
- 实现 BOSS Mapper、Hub Client 和 Outbox。
- 实现职位列表、详情和快照查询 API。
- 完成 Collector、Outbox、Information Hub、MySQL 和查询 API 的真实端到端验收。

## 当前可用链路

```text
BOSS Collector
→ 列表与详情合并
→ InformationEnvelope V1
→ Information Hub Client
→ 接入 API
→ MySQL
→ Job Query API V1
```

## Phase 2 已完成任务

- 完成 TASK-011：冻结标准化职位 Web 浏览 MVP 范围，接受 Web UI Behavior V1 和 ADR-010。
- 完成 TASK-012：创建可安装、启动、测试、类型检查和构建的 Vue 3 项目骨架，提供 `/jobs` 和 404 占位页。
- 完成 TASK-013：实现统一 Axios Client、职位查询类型、三个只读 API 函数、错误映射、格式化工具和 Vite `/api` Proxy。
- 完成 TASK-014：实现标准化职位列表、全部合同筛选项、排序、分页、URL 状态恢复、请求取消和乱序保护，以及 Loading、Empty、Error 和重试。
- 完成 TASK-015：实现标准化职位详情、纯文本 JD、安全来源链接、返回列表、快照入口、请求取消，以及 Loading、404、Error 和重试。
- 完成 TASK-016：实现历史快照列表、非连续版本选择、正文和标准化 JSON 查看、返回导航、请求取消，以及 Loading、Empty、404、Error 和重试。

## 当前任务

TASK-017：完善交互、响应式和健壮性。

TASK-017 将统一职位列表、详情和历史快照页面的布局与状态体验，修复窄屏、长文本、异常数据、键盘操作和跨页面一致性问题；不新增业务模块或后端接口。

## Phase 2 已冻结范围

包含：

- 职位列表；
- 搜索、筛选、排序和分页；
- 职位详情；
- 历史快照；
- Loading、Empty、Error 和 404；
- URL 状态恢复；
- 前端测试；
- 受控构建和部署配置。

不包含：

- rawPayload；
- 用户登录和复杂权限；
- 职位写操作；
- Collector 管理；
- AI；
- 推荐；
- 通知；
- 公网无认证部署。

## 当前设计约束

- 前端只调用 Information Hub API。
- Job Query API V1 是 Phase 2 的后端数据契约。
- 列表、详情和快照不返回 rawPayload。
- 开发环境优先使用 Vite Proxy。
- 部署环境优先使用 Nginx 同源代理。
- 未增加读取认证前，只允许受控访问。
- 前端不保存 Collector Token。
- 不修改 Phase 1 的幂等、快照和归档语义。
- 列表状态写入 URL Query；详情和快照使用经过校验的内部 `from` 参数恢复列表。
- 重置筛选时保留当前 `size`，其他状态恢复默认值。
- 不提前进入 Phase 3。

## 下一步

执行 TASK-017。开始修改前先审查三个核心页面的问题清单，确定通用状态组件边界、窄屏和长文本方案、键盘与 ARIA 检查，以及必要回归测试；修复优先，不先进行大规模重构。
