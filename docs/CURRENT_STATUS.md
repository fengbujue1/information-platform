# 当前开发状态

更新时间：2026-07-29  
当前分支：dev

## 当前阶段

Phase 1 已完成。

Phase 2 范围已冻结，尚未创建 Vue 业务代码。

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

## 当前任务

TASK-012：创建 Vue 3 项目骨架。

TASK-012 只创建可安装、启动、测试和构建的前端骨架，不提前实现职位页面。

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

执行 TASK-012，创建 Vue 3 项目骨架；开始前先检查本机 Node 环境并冻结受维护的 LTS 版本。
