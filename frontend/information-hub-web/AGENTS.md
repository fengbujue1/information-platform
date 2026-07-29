# Information Hub Web 开发规则

## 一、模块定位

本模块负责标准化信息的浏览前端。

Phase 2 当前只实现职位浏览 MVP。

前端不得直接访问：

- MySQL；
- Collector；
- Collector 本地文件；
- 大模型；
- Outbox 文件。

所有业务数据通过 Information Hub API 获取。

## 二、技术栈

- Vue 3
- TypeScript
- Vite
- Vue Router
- Element Plus
- Pinia
- Axios
- Vitest
- Vue Test Utils

Phase 2 默认使用 npm，并提交 `package-lock.json`。未经确认不得在同一项目中混用 npm、pnpm 和 yarn。

Node 版本必须使用受维护的 LTS，并通过仓库版本文件固定。实际版本由 TASK-012 在检查开发环境后确定。

## 三、Phase 2 范围

允许：

- 职位列表；
- 筛选、排序和分页；
- 职位详情；
- 历史快照；
- Loading、Empty、Error 和 404；
- URL 状态恢复；
- 前端测试；
- 受控构建部署。

禁止提前实现：

- rawPayload；
- 用户系统；
- 复杂权限；
- 收藏和备注；
- 职位写操作；
- Collector 管理；
- AI、推荐和通知；
- 公网无认证部署。

## 四、目录边界

推荐：

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

职责：

- `src/api/`：Axios 实例和 API 函数。
- `src/types/`：接口和页面类型。
- `src/views/`：路由页面。
- `src/components/`：通用展示组件。
- `src/composables/`：可复用组合逻辑。
- `src/stores/`：真正跨页面的状态。
- `src/utils/`：无副作用格式化和校验函数。

## 五、API 规则

1. 页面中不直接创建 Axios 实例。
2. 请求和响应必须有 TypeScript 类型。
3. 类型必须以 Job Query API V1 和后端实际响应为准。
4. 禁止使用 `any` 跳过已知响应结构。
5. 排序字段必须使用后端白名单。
6. 列表、详情和快照均不请求 rawPayload。
7. 错误码统一转换为用户可理解的信息。
8. 请求取消、重复请求和竞态必须得到处理。
9. 不在浏览器中保存 Collector Token。

## 六、路由和状态

- 默认入口 `/jobs`。
- 详情 `/jobs/:id`。
- 快照 `/jobs/:id/snapshots`。
- 列表筛选、排序和分页写入 URL Query。
- 刷新和浏览器前进后退不能丢失状态。
- 从详情返回列表时恢复原列表 URL。

## 七、安全

- 不使用 `v-html` 渲染 JD。
- 外部链接必须使用 `noopener noreferrer`。
- `.env` 真实文件不得提交 Git。
- 不把服务器秘密写入 `VITE_` 环境变量。
- 未增加读取认证前，不配置公网无认证访问。
- 不增加宽泛的后端 CORS；优先使用 Vite Proxy 和 Nginx 同源代理。

## 八、用户体验

必须覆盖：

- 正常列表；
- 空列表；
- 请求失败；
- 网络断开；
- 字段为空；
- 长标题；
- 长正文；
- 快照为空；
- 多历史版本；
- 404；
- 非法 URL 参数；
- 移动端和窄屏基础适配。

## 九、测试

每个任务至少运行适用的：

```text
npm run typecheck
npm run test
npm run build
```

组件和工具函数应有稳定测试。

TASK-019 负责端到端验收，不能用单元测试替代真实前后端联调。

## 十、任务纪律

- 只执行当前 TASK。
- 修改前先列出文件、步骤、测试和风险。
- 不顺手增加后端字段。
- 发现 API 缺口时先记录，不猜测响应。
- 不为了未来需求创建复杂抽象。
- 完成后更新 TASK 和 CURRENT_STATUS。
- Codex 不提交 Git，等待用户检查。
