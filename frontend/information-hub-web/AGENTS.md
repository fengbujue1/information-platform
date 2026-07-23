# Information Hub Web 开发规则

## 一、模块定位

本模块负责信息浏览和管理前端。

前端不得直接访问 MySQL、Collector 或大模型，所有业务数据通过 Information Hub API 获取。

## 二、技术栈

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Axios

实际项目创建后，以 `package.json` 和 lock 文件为准，不得猜测 npm、pnpm 或 yarn。

## 三、目录边界

- `src/api/`：HTTP 请求。
- `src/types/`：协议类型。
- `src/views/`：页面。
- `src/components/`：通用组件。
- `src/stores/`：跨页面状态。

## 四、API 规则

- 页面中不直接创建 Axios 实例。
- 请求和响应必须有 TypeScript 类型。
- 排序字段必须使用后端允许的白名单。
- 列表默认不请求 rawPayload。
- 详情页按需查看原始数据和历史快照。

## 五、测试场景

至少验证：

- 正常列表；
- 空列表；
- 请求失败；
- 可选字段为空；
- 分页最大值；
- 非法排序字段；
- 长标题和长正文；
- 历史快照为空和多版本。
