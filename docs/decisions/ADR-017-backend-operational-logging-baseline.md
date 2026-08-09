# ADR-017：Phase 4 建立 Backend Operational Logging Baseline

状态：Accepted

实施状态：Implemented and verified by TASK-044

## 背景

当前开发中，部分失败只能在浏览器 F12 看到，后端缺少可定位的流程和失败日志。

Phase 4 将新增 Recommendation Run、Worker、Trigger，因此先补统一日志。

## 决策

Phase 4 第一个 TASK 完成：

- SLF4J + Logback；
- timestamp；
- requestId MDC；
- authenticated username MDC；
- HTTP lifecycle；
- validation/business WARN；
- system ERROR + stack trace；
- durationMs；
- async MDC；
- Prompt / Analysis / Batch / Provider / Schedule / Collector / Archive / Recommendation / Interaction 关键节点。

详细规范：

`docs/LOGGING_CONVENTIONS.md`

无用户上下文时不伪造 username。

不建设复杂脱敏框架，但继续禁止 Token/Cookie/API Key/Password/完整敏感 Payload 入日志。
