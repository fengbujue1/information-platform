# TASK-033：Backend Operational Logging Baseline

状态：DONE

所属阶段：Phase 4

优先级：P0

设计状态：Accepted

## 1. 目标

在 Recommendation 功能编码前，为现有后端建立统一流程日志、失败留痕、requestId 和登录用户上下文。

## 2. 前置依赖

Phase 3 已完成；阅读 `docs/LOGGING_CONVENTIONS.md`。

## 3. 实施范围

- Logback pattern；
- requestId MDC；
- username MDC；
- HTTP start/end/status/durationMs；
- Global Exception Handler WARN/ERROR；
- Async MDC propagation；
- Prompt 日志；
- Single Analysis / Batch / Provider / Schedule 日志；
- Collector / Archive 日志；
- 测试；
- backend AGENTS 引用日志规范。

## 4. 明确不做

- ELK/Loki/Grafana；
- 复杂脱敏框架；
- Recommendation 业务实现。

## 5. 验收与测试

验证 requestId、username、system thread、validation WARN、unknown ERROR、async isolation、现有链路不回归。

## 6. 文档同步

- `backend/information-hub/AGENTS.md`
- `docs/LOGGING_CONVENTIONS.md`
- `docs/CURRENT_STATUS.md`

## 7. 完成前统一检查

- 当前 TASK 测试通过；
- `git diff --check`；
- 未超 Scope；
- 后端改动按 `docs/LOGGING_CONVENTIONS.md` 检查；
- 更新当前 TASK 实施记录；
- 更新 `docs/CURRENT_STATUS.md`；
- 如数据库/架构事实改变，同步事实文档；
- 汇报实际命令与结果。

## 8. 实施记录（2026-08-09）

- 新增统一 Logback 输出格式，并为每个 HTTP 请求生成服务端 `requestId`，写入 MDC 与响应头；
- 从可信 Spring Security Context 写入登录用户名 MDC，Collector、Scheduler、Worker 等系统链路不伪造用户；
- 新增异步任务 MDC 传播与清理，避免线程池上下文串扰；
- 补齐 HTTP、认证与 CSRF、全局 4xx/5xx、Collector/Archive、Prompt、Single Analysis、Provider、Batch 和 Schedule 的关键节点日志；
- 对数据库等可能携带敏感参数的异常保留异常类型与 stack trace，同时移除原始异常消息、cause 与 suppressed 内容；
- 未记录 Authorization、Token、Cookie、API Key、密码、Prompt 正文、完整 AI 输入输出、Collector raw payload 或 HTTP body；
- 未修改数据库、Contract、Recommendation 业务或前端。

验证结果：

- TASK-033 日志基础设施及 Identity 集成测试：15 项通过；
- 相关后端回归测试：40 项通过；`InformationIngestionIntegrationTest` 的 5 项因本机 `127.0.0.1` MySQL/SSH 隧道未开放而未能连接数据库；
- 非数据库全量回归发现 2 项既有 AI Provider token 上限断言仍期望 `5000`，而当前生产代码约束为 `1000`；该差异不由 TASK-033 引入，未在本任务越界修改；
- `git diff --check` 与 `git diff --cached --check` 在收尾阶段执行。
