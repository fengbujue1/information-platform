# Information Platform 项目背景

## 项目目标

建设一个通用的信息采集、归档、分析、推荐和浏览平台。

系统当前从 BOSS 职位信息开始，但最终模型不能绑定招聘业务。未来可能接入新闻、政府政策、房价、教育、行业报告和其他公开信息源。

## 当前基础

仓库中已有可运行的 Python BOSS 采集器：

`collectors/boss-zhipin-scraper`

该采集器使用真实 Chrome 和 Chrome DevTools Protocol 复用登录状态，通过 BOSS 搜索接口获取结构化职位列表，并抓取职位详情。

## 长期核心流程

Collector  
→ Information Hub  
→ 归档与标准化  
→ 规则过滤  
→ AI 分析  
→ 个性化推荐  
→ Web / Notification

## 长期设计原则

1. 采集器与 Information Hub 解耦。
2. 采集器不直接连接主数据库。
3. Job 只是 Information 的一种类型。
4. 原始数据必须保留。
5. 标准化字段不能取代原始数据。
6. AI 分析结果不能覆盖原始信息。
7. 信息质量分和用户匹配分必须分开。
8. 新数据源应通过统一协议接入，不应修改平台核心流程。
9. 当前先构建模块化单体，出现真实瓶颈后再分布式拆分。

## 当前技术方向

- Collector：Python
- Backend：Java 21、Spring Boot、Maven
- Persistence：MySQL 8、Flyway、MyBatis-Plus 3.5.17（Spring Boot 3 Starter）
- Frontend：Vue 3、TypeScript、Vite、Element Plus
- Deployment：Docker Compose
- AI：Phase 3 使用通用 OpenAI-compatible Client，优先复用 Spring `RestClient` 与 Jackson，不引入 AI SDK

## Java 代码可读性约定

- 新增 PO、DTO、Domain、配置对象等数据实体时，每个字段或 record 组件必须有中文注释。
- 复杂方法在定义处说明职责和关键约束；幂等、事务、合并、版本和安全等复杂调用在调用处说明业务意图。
- 注释保持简明并与实现同步，不逐行复述语法层面的代码行为。

## 开发与集成测试环境

- 开发和集成测试共用远程服务器中的 Docker MySQL。
- MySQL 使用固定版本、健康检查和持久化数据卷。
- MySQL 不直接开放公网端口，开发主机通过 SSH 隧道连接。
- 开发库和测试库相互隔离，并使用不同的非 root 最小权限账号。
- 真实密码、服务器连接信息和私钥不进入 Git。
- 生产数据库必须与该共享开发和测试环境隔离。

## 当前暂不引入

- 微服务
- Kafka / RabbitMQ
- MongoDB
- Elasticsearch
- 向量数据库
- Kubernetes
- 短信和微信推送
- 复杂用户权限系统

## Phase 3 Accepted 边界

- 第一版只实现 `JOB_USER_RELEVANCE_V1`，通用核心保持 Information 语义。
- 使用最小账号、同源 Session、CSRF 和 Owner 隔离；不实现公共注册、RBAC 或多租户。
- Prompt 跟账号走并以不可变 Version 保存；System Prompt 和 Schema 由平台控制。
- Analysis 绑定不可变 Snapshot，不覆盖来源事实，不默认读取 rawPayload。
- Actual Token 只能来自每次 Provider Invocation 的 Usage。
- Manual 与 Schedule 共用 Batch Engine；Schedule 默认关闭、用户本地 02:00。
- 第一版 Worker/Scheduler 复用 Spring + MySQL，不引入新基础设施。
- TASK-024 已实施 V2 数据库基础；TASK-021 已实施 Identity；TASK-022 已实施账号级 Prompt；TASK-023 已实施 Analysis Definition；TASK-025 已实施 Provider 与 Actual Usage 映射；TASK-026 已实施安全 Prompt Assembly、版本化执行上下文和严格结构化输出处理；TASK-027 已实施单条 Information Analysis、Invocation 与 Actual Usage 链路；TASK-028 已实施 Candidate Resolver、无 AI 调用 Preview、Token Estimate 与 HMAC 确认令牌；TASK-029 已实施 Manual Confirm、异步 Batch/Budget Guard、串行 Worker、恢复和 Usage 聚合；TASK-030 已实施每日 Schedule、IANA/DST 计划点、Scheduled Batch 幂等、misfire 和 overlap；TASK-031 已实施 Phase 3 Web 与用户维度今日/月度/累计 Actual Usage 查询。当前下一实施任务是 TASK-032 真实模型与 Phase 3 E2E 验收。
