# TASK-013：实现 API Client、类型与环境配置

状态：DONE
所属阶段：Phase 2  
优先级：P0  
负责人：User + Codex

## 1. 目标

建立统一、强类型、可测试的 Job Query API Client，为后续页面提供稳定数据访问层。

## 2. 背景

页面不能直接创建 Axios 实例，也不能在多个页面重复定义 API 类型。

## 3. 前置依赖

- TASK-012 已完成。
- Job Query API V1 已 Accepted。

## 4. 影响范围

- `frontend/information-hub-web/src/api/`
- `frontend/information-hub-web/src/types/`
- `frontend/information-hub-web/src/utils/`
- Vite 配置
- `.env.example`
- 前端测试
- 当前 TASK 和 CURRENT_STATUS

## 5. 本任务范围

- 创建统一 Axios 实例。
- 创建 API Base URL 配置。
- 配置开发环境 `/api` 代理。
- 定义统一成功和失败响应类型。
- 定义职位分页参数和响应类型。
- 定义职位详情类型。
- 定义快照类型。
- 实现：
  - `getJobs`
  - `getJobById`
  - `getJobSnapshots`
- 创建统一错误类型和错误码映射。
- 创建日期、空值和薪资基础格式化函数。
- 添加 API Client 和格式化函数测试。
- 使用 Mock Adapter、请求拦截或等效方式测试，不依赖真实后端。

## 6. 不在本任务范围

- 不创建完整职位列表 UI。
- 不创建详情 UI。
- 不创建快照 UI。
- 不修改 Job Query API。
- 不增加 rawPayload 类型。
- 不增加前端 Token。
- 不增加写操作。

## 7. 业务与技术规则

- 类型以 Job Query API V1 和后端实际响应为准。
- Nullable 字段必须保留 nullable。
- `sortBy` 使用字符串联合类型。
- URL 参数只发送有意义的值。
- 不直接向页面暴露 Axios 内部错误对象。
- API Base URL 默认支持同源 `/api`。
- 前端不配置数据库或 Collector Token。

## 8. 验收标准

- [x] 三个 API 函数类型完整
- [x] 无 `any` 绕过主要响应结构
- [x] Vite Proxy 配置可用
- [x] 错误码可统一处理
- [x] 单元测试覆盖成功、404、参数错误和网络失败
- [x] typecheck、test、build 通过
- [x] CURRENT_STATUS 更新为 TASK-014

## 9. 实施前计划

Codex 必须先对照：

- Job Query API V1
- 后端 DTO
- 后端 Controller
- 真实响应测试

发现契约不一致时先汇报，不得自行猜测。

实施前已完成契约、Controller、DTO、数据库约束和 Controller 响应测试核对，并经用户确认后开始修改。

## 10. 实施记录

- 以 Accepted 的 Job Query API V1、后端 `ApiResponse`、分页 DTO、职位 DTO 和 Controller 测试为类型事实来源。
- 对照数据库字段约束保留 nullable；来源、标题、采集时间、版本号和状态等必填字段保持非 nullable。
- 新增递归 `JsonValue` 类型，承载标签数组和 `standardizedPayload`，未使用 `any`，未声明 rawPayload。
- 建立唯一 Axios 实例，默认同源 Base URL 为 `/api`，请求超时为 10 秒，并统一发送 JSON Accept Header。
- 实现 `getJobs`、`getJobById` 和 `getJobSnapshots` 三个只读函数。
- 列表参数只发送有限数值和非空文本；文本在发送前去除首尾空格，数值 `0` 保留。
- 三个查询函数均支持可选 `AbortSignal`，为后续页面取消过期请求提供基础。
- 建立 `ApiClientError`，区分业务错误、网络错误、超时、取消、协议错误和未知错误。
- 后端稳定错误码映射为中文信息，不向页面暴露 Axios request、response 或 config。
- 日期使用 `Intl.DateTimeFormat` 按浏览器本地时区显示，不固定增加 8 小时。
- 薪资优先展示 `salaryText`，缺失时才使用标准化月薪区间和发薪月数。
- 增加 `VITE_API_BASE_URL=/api` 和仅供 Vite Server 使用的 `INFORMATION_HUB_PROXY_TARGET`。
- Vite `/api` Proxy 保留原路径，默认目标为 `http://127.0.0.1:8080`，支持通过未提交的本地环境文件覆盖。
- 新增 `axios-mock-adapter` 纯测试依赖，所有 API Client 测试均不访问真实后端。
- 未创建职位业务页面、详情路由、业务 Store、写操作、认证或原始数据类型。
- 未修改 Java、Python、数据库或 Collector。

## 11. 测试结果

- `npm.cmd ci --prefer-offline --no-audit --no-fund --no-update-notifier`：通过，按最终锁文件安装 205 个包。
- `npm.cmd run typecheck`：通过。
- `npm.cmd run test`：通过，4 个测试文件、18 项测试全部通过。
- API测试覆盖分页、详情、快照成功响应，404、参数错误、网络错误和非法响应。
- 格式化测试覆盖空值、数值零、UTC时间、无效时间、来源薪资文本和标准化薪资。
- Proxy测试覆盖自定义目标、本机默认目标和路径不重写。
- `npm.cmd run build`：通过；主要 JS 产物约 131.84 kB，gzip 后约 49.75 kB。
- `npm.cmd audit --audit-level=high`：通过，0 个已知漏洞。

## 12. 遗留问题

- TASK-014 再让 `/jobs` 页面实际调用 `getJobs`，实现 URL 状态、筛选、分页和页面状态。
- 当前默认 Proxy Target 假设 Information Hub 监听本机 `8080`；不同端口通过 `.env.local` 覆盖。
- 当前 API ID 按后端 JSON 实际行为使用 TypeScript `number`；如未来超过 JavaScript 安全整数范围，需要先变更后端契约。
- 未知后端错误码使用统一通用提示，不直接显示后端内部信息。
- 当前页面仍为占位页，不会自动访问真实 Information Hub，符合本任务边界。

## 13. 完成确认

- [x] CURRENT_STATUS 已更新
- [ ] 用户已检查 git diff
- [ ] 用户已确认测试结果
- [x] 新增文件已加入 Git 追踪
- [x] 未提交 Git，等待用户检查
