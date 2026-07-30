# 当前开发状态

更新时间：2026-07-30
当前分支：dev

## 当前阶段

Phase 1 已完成。

Phase 2 标准化职位 Web 浏览 MVP 已完成，并通过自动化浏览器测试和项目负责人的真实环境人工端到端验收。

当前等待 Phase 3 范围规划，尚未开始 AI、推荐或通知实现。

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
→ Information Hub Web
```

## Phase 2 已完成任务

- 完成 TASK-011：冻结标准化职位 Web 浏览 MVP 范围，接受 Web UI Behavior V1 和 ADR-010。
- 完成 TASK-012：创建可安装、启动、测试、类型检查和构建的 Vue 3 项目骨架，提供 `/jobs` 和 404 占位页。
- 完成 TASK-013：实现统一 Axios Client、职位查询类型、三个只读 API 函数、错误映射、格式化工具和 Vite `/api` Proxy。
- 完成 TASK-014：实现标准化职位列表、全部合同筛选项、排序、分页、URL 状态恢复、请求取消和乱序保护，以及 Loading、Empty、Error 和重试。
- 完成 TASK-015：实现标准化职位详情、纯文本 JD、安全来源链接、返回列表、快照入口、请求取消，以及 Loading、404、Error 和重试。
- 完成 TASK-016：实现历史快照列表、非连续版本选择、正文和标准化 JSON 查看、返回导航、请求取消，以及 Loading、Empty、404、Error 和重试。
- 完成 TASK-017：统一页面壳和状态反馈，完善窄屏分页、长内容保护、键盘焦点、基础 ARIA 和跨页面健壮性测试。
- 完成 TASK-018：建立 Java、Python、Vue 基础 CI，提供固定版本 Web 镜像、Nginx SPA 回退、`/api` 同源代理、部署校验和受控访问文档。
- 完成 TASK-019：增加稳定 Fixture 驱动的浏览器 E2E，完成真实环境人工联调和 Phase 2 文档收尾。

## 当前任务

当前没有活动实施任务。

项目等待 Phase 3 规划任务。开始 AI 业务代码前，必须先确定 Phase 3 的目标、范围、输入输出协议、数据模型、安全边界、模型接入方式、成本限制和验收标准。

## Phase 2 完成能力

- 职位列表；
- 搜索、筛选、排序和分页；
- 职位详情；
- 历史快照；
- Loading、Empty、Error 和 404；
- URL 状态恢复；
- 前端单元、组件和浏览器 E2E；
- 受控构建和部署配置；
- 浏览器到真实 MySQL 的完整只读链路验收。

## 当前设计约束

- 前端只调用 Information Hub API。
- Job Query API V1 是 Web 浏览的后端数据契约。
- 列表、详情和快照不返回 rawPayload。
- 开发环境优先使用 Vite Proxy。
- 部署环境优先使用 Nginx 同源代理。
- 未增加读取认证前，只允许本机、SSH 隧道或受控网络访问。
- Web Compose profile 默认只绑定宿主机回环地址，不公开无认证读取 API。
- 前端不保存 Collector Token。
- CI 不使用私人 SSH 密钥，不连接远程共享 MySQL。
- 不修改 Phase 1 的幂等、快照和归档语义。
- Phase 3 未完成设计冻结前，不提前实现 AI、推荐或通知。

## 下一步

创建 Phase 3 规划任务。先完成需求澄清、架构影响评估和设计冻结，再拆分最小实施任务。
