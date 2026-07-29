# TASK-013：实现 API Client、类型与环境配置

状态：TODO  
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

- [ ] 三个 API 函数类型完整
- [ ] 无 `any` 绕过主要响应结构
- [ ] Vite Proxy 配置可用
- [ ] 错误码可统一处理
- [ ] 单元测试覆盖成功、404、参数错误和网络失败
- [ ] typecheck、test、build 通过
- [ ] CURRENT_STATUS 更新为 TASK-014

## 9. 实施前计划

Codex 必须先对照：

- Job Query API V1
- 后端 DTO
- 后端 Controller
- 真实响应测试

发现契约不一致时先汇报，不得自行猜测。

## 10. 实施记录

待填写。

## 11. 测试结果

待填写。

## 12. 遗留问题

待填写。

## 13. 完成确认

- [ ] CURRENT_STATUS 已更新
- [ ] 用户已检查 git diff
- [ ] 用户已确认测试结果
- [ ] 已提交并 push
