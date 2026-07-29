# Phase 2：标准化职位 Web 浏览 MVP

状态：Accepted
接受日期：2026-07-29
计划开始日期：2026-07-29
负责人：User + Codex

## 1. 背景

Phase 1 已完成以下链路：

```text
BOSS Collector
→ InformationEnvelope V1
→ Information Hub
→ MySQL 当前版本与历史快照
→ Job Query API V1
```

当前后端已经提供：

```http
GET /api/v1/jobs
GET /api/v1/jobs/{id}
GET /api/v1/jobs/{id}/snapshots
```

前端目录目前只有开发规则，尚未创建实际 Vue 项目。

现有 Job Query API 只返回标准化字段，不返回 `rawPayload`，也没有增加读取端用户认证。

## 2. Phase 2 目标

建设一个可实际使用的 Web MVP，使项目负责人能够在浏览器中：

1. 浏览已经归档的职位。
2. 按条件搜索、筛选、排序和分页。
3. 查看职位当前详情。
4. 查看职位历史版本。
5. 从职位详情跳转到来源页面。
6. 在接口异常、数据为空和字段缺失时获得明确反馈。
7. 刷新页面或复制 URL 后保留当前筛选和页面状态。

Phase 2 的价值是把 Phase 1 的后端数据链路转化为可浏览的产品界面。

## 3. 目标用户

Phase 2 MVP 的目标用户是：

- 项目负责人本人；
- 受信任的开发和测试人员。

当前不把它定义为面向公众的招聘网站。

## 4. 必须完成的功能

### 4.1 职位列表

展示：

- 职位标题；
- 公司名称；
- 薪资；
- 地点；
- 工作经验；
- 学历；
- 办公方式；
- 来源职位状态；
- 首次发现时间；
- 最近发现时间；
- 当前版本号。

支持：

- 关键词；
- 公司；
- 城市；
- 最低月薪；
- 最高月薪；
- 来源；
- 职位状态；
- 办公方式；
- 排序字段；
- 排序方向；
- 页码；
- 每页数量。

### 4.2 职位详情

展示现有 Job Query API 提供的标准化字段：

- 标题和来源链接；
- 公司和公司信息；
- 薪资；
- 地点；
- 经验和学历；
- 招聘者职位和在线观测信息；
- 职位状态和详情抓取状态；
- 来源标签；
- 来源技能标签；
- 福利；
- 当前 JD；
- 首次和最近发现时间；
- Collector 标识和版本；
- 当前版本号。

### 4.3 历史快照

展示：

- 版本号；
- 快照创建时间；
- 采集时间；
- 内容 Hash；
- 该版本标题；
- 该版本正文；
- 标准化业务 JSON；
- Collector 版本。

Phase 2 只实现版本选择和内容查看，不要求实现复杂的逐字 Diff 算法。

### 4.4 通用页面状态

必须具备：

- Loading；
- 空数据；
- API 错误；
- 404；
- 参数错误；
- 网络断开；
- 字段为空；
- 长标题和长正文；
- 重试入口。

## 5. 路由范围

建议路由：

```text
/jobs
/jobs/:id
/jobs/:id/snapshots
```

允许在职位详情页内使用标签页展示历史快照，但 URL 必须能够直接定位到对应职位和历史页面。

默认入口：

```text
/jobs
```

## 6. URL 状态规则

职位列表的以下状态必须写入 URL Query：

- page
- size
- keyword
- company
- city
- salaryMin
- salaryMax
- source
- jobStatus
- remoteType
- sortBy
- sortDirection

要求：

1. 刷新页面后状态不丢失。
2. 浏览器前进和后退行为正确。
3. 从详情返回列表时，通过经过校验的内部 `from` 参数恢复原筛选条件和页码。
4. 非法参数使用安全默认值，并向用户显示必要提示。
5. 前端参数必须遵守 Job Query API V1 白名单。
6. `from` 只允许指向以 `/jobs` 开头的站内地址；缺失或非法时返回 `/jobs`。
7. 重置筛选时保留当前 `size`，其他筛选、排序和页码恢复默认值。

## 7. API 和网络边界

### 7.1 API 来源

前端只调用 Information Hub API。

禁止：

- 直接访问 MySQL；
- 直接访问 BOSS；
- 读取 Collector 本地文件；
- 在浏览器中保存数据库密码或 Collector Token。

### 7.2 开发环境

推荐使用 Vite Dev Server Proxy：

```text
浏览器
→ Vite /api 代理
→ Information Hub
```

避免为了本地开发在后端配置宽泛 CORS。

### 7.3 部署环境

推荐使用 Nginx 同源部署：

```text
浏览器
→ Nginx
   ├── 静态 Web 文件
   └── /api → Information Hub
```

未增加读取认证前：

- 不允许把 Web 和读取 API 直接公开到公网；
- 只允许本机、可信网络、SSH 隧道或其他受控访问方式；
- 公网访问需要新增独立认证和部署安全任务。

## 8. rawPayload 处理

Phase 2 不查看：

- `information_item.raw_payload`
- `information_snapshot.raw_payload`
- 采集凭证；
- Cookie；
- Token；
- `security_id`；
- `lid`。

原因：

1. 当前 Job Query API V1 明确不返回 rawPayload。
2. 当前没有管理端读取认证。
3. 原始数据可能包含来源特有或未来新增的敏感字段。
4. 为了 rawPayload 查看而提前建设权限系统会导致 Phase 2 范围膨胀。

未来需要 rawPayload 管理功能时，应单独创建：

- 管理端认证；
- 原始数据脱敏；
- 审计日志；
- 独立管理接口；
- 权限规则。

## 9. 非功能要求

### 9.1 技术栈

- Vue 3
- TypeScript
- Vite
- Vue Router
- Element Plus
- Pinia
- Axios
- Vitest
- Vue Test Utils

端到端测试工具在 TASK-019 中按最小需要引入。

### 9.2 TypeScript

- API 请求和响应必须定义明确类型。
- 禁止用 `any` 代替已知 API 数据结构。
- Nullable 字段必须按契约处理。
- 页面不能自行猜测后端字段。

### 9.3 代码结构

建议：

```text
src/
├── api/
├── components/
├── composables/
├── layouts/
├── router/
├── stores/
├── types/
├── utils/
└── views/
```

### 9.4 安全

- 不使用 `v-html` 渲染职位正文。
- 外部来源链接使用 `rel="noopener noreferrer"`。
- 不把 Token、密码或服务器地址写死到前端代码。
- `.env` 真实文件不得提交 Git。
- 前端不能包含 Collector Bearer Token。
- 不新增职位修改、删除或管理接口。

### 9.5 可维护性

- API 调用集中到 `src/api/`。
- 页面组件不直接创建 Axios 实例。
- 日期、薪资、空值和错误信息使用统一格式化函数。
- 通用 Loading、Empty 和 Error 状态抽取为组件。
- Pinia 只用于真正跨页面的状态，不把所有局部状态放入 Store。

## 10. 明确不在 Phase 2 范围

- rawPayload 管理；
- 用户注册、登录和找回密码；
- 角色权限和多租户；
- 职位收藏；
- 职位备注；
- 职位编辑和删除；
- Collector 启停管理；
- Outbox Web 管理；
- AI 分析；
- 用户画像；
- 个性化推荐；
- 消息通知；
- Elasticsearch；
- Redis；
- 新数据库表；
- 微服务；
- Kubernetes；
- 面向公众的无认证部署；
- 移动 App；
- 小程序。

出现以上需求时，必须先执行范围变更评估，不得顺手加入当前 TASK。

## 11. Phase 2 完成标准

Phase 2 完成必须同时满足：

1. Vue 项目可以安装、启动、测试和构建。
2. 职位列表可访问真实 Job Query API。
3. 筛选、排序、分页与 URL 状态一致。
4. 职位详情可以直接访问和刷新。
5. 历史快照可以查看。
6. 所有页面有 Loading、Empty 和 Error 状态。
7. API 类型与 Job Query API V1 一致。
8. 不读取或暴露 rawPayload。
9. 不包含数据库密码、Collector Token 或其他秘密。
10. 前端单元和组件测试通过。
11. 基础端到端流程通过。
12. 生产构建通过。
13. 文档和实际代码一致。
14. README、ROADMAP 和 CURRENT_STATUS 已更新。
15. 未提前实施 Phase 3。

## 12. Phase 2 任务

```text
TASK-011 冻结 Phase 2 Web MVP 范围
TASK-012 创建 Vue 3 项目骨架
TASK-013 实现 API Client、类型与环境配置
TASK-014 实现职位列表、筛选和分页
TASK-015 实现职位详情
TASK-016 实现历史快照查看
TASK-017 完善交互、响应式和健壮性
TASK-018 建立 CI 与受控构建部署配置
TASK-019 完成 Phase 2 端到端验收
```

## 13. 启动前待用户确认

- [x] Phase 2 只浏览标准化字段。
- [x] rawPayload 推迟到管理端认证之后。
- [x] 当前不建设用户系统。
- [x] 当前不公开部署无认证读取 API。
- [x] 开发使用 Vite Proxy。
- [x] 部署优先使用 Nginx 同源代理。
- [x] 历史快照先做版本查看，不做复杂 Diff。
- [x] 路由固定为 `/jobs`、`/jobs/:id`、`/jobs/:id/snapshots`。
- [x] 列表状态写入 URL Query，并使用受校验的内部 `from` 参数恢复列表。
- [x] 重置筛选时保留 `size`。
- [x] 先执行 TASK-011，不立即创建 Vue 代码。
