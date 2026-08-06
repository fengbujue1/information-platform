# TASK-031：实现 Phase 3 Web

状态：DONE
所属阶段：Phase 3
优先级：P0
负责人：User + Codex

## 1. 目标

在现有 Vue 3 Web 中实现登录、Prompt、手动分析、Batch、Schedule、Analysis Result 和 Token Usage 页面，形成可实际使用的 Phase 3 产品界面。

## 2. 前置依赖

- TASK-030 已完成并推送。
- 已阅读 `docs/PHASE3_SCOPE.md` 和本任务相关 Contract / ADR。
- 实施基线为 `origin/dev` 的 `4e26ebcadfd361a5604b726298c638b8b11bfe01`。

## 3. 本任务范围

已实现路由：

- `/login`
- `/ai/prompts`
- `/ai/analyze`
- `/ai/analyses/:id`
- `/ai/batches`
- `/ai/batches/:id`
- `/ai/schedules`
- `/ai/usage`
- 职位详情中的当前 Information Analysis 入口。

已实现页面能力：

- Login / Logout / Me 和认证路由保护；
- Prompt Profile 创建、查询、启停；
- 不可变 Prompt Version 创建、历史查看和 Active Version 切换；
- Manual Preview、total/eligible/already/pending、Estimated Token；
- 使用服务端 HMAC Preview Token 显式 Confirm Batch；
- Batch 列表、详情、轮询进度、Items、Estimate/Actual Usage；
- 单条 Analysis 执行和结构化 Analysis Result；
- 今日、本月、累计用户 Actual Token；
- Schedule 创建、修改、独立启停、默认关闭、默认 02:00；
- Schedule timezone、windowDays、limits、Preview、上次/下次执行；
- Loading、Empty、Error、401/403 和强类型 API Client；
- 同源 Session、CSRF 和 Job 详情 Analysis 入口。

## 4. 用户 Usage API 补齐

实施前审查确认：数据模型允许按 `ai_model_invocation.user_id` 聚合，但 TASK-027～030 没有提供用户维度 Usage HTTP API，单靠现有 Batch 接口无法准确实现今日、本月和累计验收。

经用户确认，本任务增加：

```http
GET /api/v1/ai/usage
```

响应包含：

- `timezone`；
- `today`；
- `month`；
- `allTime`；
- 每个范围的 Invocation、REPORTED/UNAVAILABLE 数量；
- Provider Actual input/output/total token。

实现约束：

- Owner 只来自当前认证 Session；
- 今日和本月按账号 IANA 时区计算，再转换为 UTC 半开区间；
- 时间归属使用现有 `ai_model_invocation.created_at` 和 `(user_id, created_at)` 索引；
- Actual Token 仅聚合 `usage_status=REPORTED` 的 Provider 字段；
- 不使用 Estimated Token 补算 Actual；
- 不新增 Usage 汇总表或 Migration；
- 只读 GET 不要求 CSRF，但仍要求 Session。

## 5. 实施记录

### 后端

- 增加 `analysis.usage` 模块的 Controller、DTO、Service、Domain、Mapper Row 和 Mapper；
- 增加 Owner + UTC 时间范围的 MySQL 实时聚合；
- 增加账号时区边界、空 Usage、401 和只读 GET 测试；
- 扩展 Phase 3 真实 MySQL Mapper 测试，验证只聚合 Provider 实际报告值；
- 修复 TASK-030 新增的测试 `application.yml` 遮蔽主配置后，Session Cookie 测试属性缺失的问题：测试资源显式保留 HttpOnly、SameSite、Secure=false 和 path；
- 未修改生产 Session Cookie 配置。

### 前端

- 增加 Analysis、Batch、Schedule、Usage 强类型和 API Client；
- 写请求统一通过既有 CSRF 协商 Token；
- AI 页面采用路由级动态加载，避免 Phase 3 页面全部进入首屏 JS；
- Prompt UI 只创建新 Version，不修改历史正文；
- Preview 与 Confirm 明确分步，Preview 页面明确标注不产生 Actual Token；
- Batch Detail 每 2.5 秒查询 Progress，终态或卸载时停止轮询；
- Analysis Result 以文本/JSON 安全展示，不使用 `v-html`；
- Schedule 配置保存与启停分离，创建强制默认关闭；
- Usage 页面明确区分 Actual 与 Estimated，并保留 UNAVAILABLE/NULL；
- 未向浏览器暴露 Provider Key、Collector Token、数据库凭据或 Session 内容。

### 数据流与边界

```text
Web
→ same-origin Session / CSRF
→ Prompt / Analysis / Preview / Batch / Schedule API
→ Existing Phase 3 services
→ MySQL

Provider Usage
→ ai_model_invocation
→ Owner + user timezone aggregate
→ GET /api/v1/ai/usage
→ Today / Month / All-time Web cards
```

Preview 不创建 Invocation；Confirm 只传服务端签名的 `previewToken`。Provider HTTP、Worker 和 Schedule 的既有事务边界未改变。

## 6. 验收标准

- [x] 登录状态工作正常
- [x] Prompt Version UI 不覆盖历史
- [x] Preview 不产生 Actual Token
- [x] 用户可确认并创建 Batch
- [x] Batch 可查看进度
- [x] Analysis Result 可读
- [x] 今日/月度/累计 Token 可见
- [x] Schedule 默认关闭
- [x] 默认时间显示 02:00
- [x] Schedule 测试 Preview 可用
- [x] 401/403 正确处理
- [x] 无秘密进入浏览器
- [x] typecheck/test/build 通过
- [x] CURRENT_STATUS 指向 TASK-032

## 7. 测试结果

环境：

- Java `21.0.11`；
- Node `24.18.x` / npm `11.16.x`（满足仓库 engines）；
- 真实测试 MySQL 通过本机 `127.0.0.1:13306` SSH 隧道访问。

后端：

- `.\mvnw.cmd "-Dtest=AnalysisUsageServiceTest,AnalysisUsageControllerTest" test`
  - 4 项，0 失败，0 错误，0 跳过；
- `.\mvnw.cmd "-Dtest=AnalysisUsageServiceTest,AnalysisUsageControllerTest,Phase3MapperIntegrationTest" test`
  - 5 项，0 失败，0 错误，0 跳过；
- `.\mvnw.cmd "-Dtest=IdentitySecurityIntegrationTest" test`
  - 7 项，0 失败，0 错误，0 跳过；
- `.\mvnw.cmd test`
  - 175 项，0 失败，0 错误，1 跳过，`BUILD SUCCESS`；
  - 跳过项是未配置独立空库环境变量的 `Phase3EmptySchemaMigrationIntegrationTest`。

前端：

- `npm.cmd run typecheck`
  - 通过；
- `npm.cmd test`
  - 23 个测试文件，78 项测试，0 失败；
- `npm.cmd run build`
  - typecheck 与 Vite production build 通过；
  - AI 路由拆分为独立 chunks，无大于 500 kB 的 JS chunk 警告。

未执行：

- 真实 Provider、真实定时触发和完整浏览器 Phase 3 E2E；这些属于 TASK-032。

## 8. 遗留问题

- MySQL 8.4 继续出现 Flyway“最新已测试版本为 MySQL 8.1”的提示，Migration 校验和测试均成功。
- Element Plus 全局 CSS 仍进入公共 CSS bundle；不影响本任务功能，后续只有出现真实首屏性能问题时再评估按组件 CSS。
- 当前 Batch 列表使用后端现有 `limit`，没有新增分页 Contract。
- 真实模型 Actual Usage 一致性、失败调用 Usage 和 Schedule 实际触发留给 TASK-032。

## 9. 明确未实施

- 浏览器保存或直接调用 Provider；
- 充值、套餐、金额或账单；
- 推荐、Top N、通知；
- 公共注册、OAuth、RBAC、多租户；
- Prompt Version 删除或历史修改；
- System Prompt / Output Schema 编辑；
- Usage 汇总表、Preview 表、Schedule Run 表、Spring Session JDBC；
- Kafka、Redis、Elasticsearch、Vector DB、RAG、Agent、微服务；
- 真实 Provider 和 Phase 3 最终 E2E。

## 10. 完成确认

- [x] 当前 TASK 文档已更新
- [x] `docs/CURRENT_STATUS.md` 已更新
- [x] `git diff --check` 通过
- [x] 用户已检查 `git diff`
- [x] 用户确认测试结果
- [x] 用户完成 commit / push
