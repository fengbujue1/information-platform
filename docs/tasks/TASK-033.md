# TASK-033：Backend Operational Logging Baseline

状态：TODO

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
