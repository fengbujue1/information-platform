# Information Hub Backend Operational Logging Conventions

状态：Accepted

实施状态：TASK-033 已完成基础能力落地；TASK-044 全栈验收确认秘密与 raw payload 未进入日志

适用范围：`backend/information-hub`

Phase 4 起强制执行。

## 1. 目标

后端日志必须能够判断：

- 请求是否到达；
- 哪个登录用户触发；
- 校验/业务规则为什么拒绝；
- Prompt/Collector/Analysis/Batch/Recommendation 执行到哪里；
- AI Provider 是否请求/返回；
- duration；
- 系统异常位置。

重点记录：

```text
业务节点
状态
业务 ID
失败原因
数量
durationMs
```

## 2. 技术

统一：

- SLF4J；
- Logback；
- MDC；
- Spring Security Authentication；
- HTTP Filter / Interceptor；
- TaskDecorator / Executor MDC propagation。

不用 System.out/err 作为业务日志。

## 3. Context

### timestamp

Logback：

```text
yyyy-MM-dd HH:mm:ss.SSS
```

### requestId

每个 HTTP Request 统一 requestId，进入 MDC，请求完成必须清理。

### username

有实际 Session 登录用户时从 Security Context 放入 MDC。

没有用户上下文时不伪造：

- Scheduler；
- Collector Bearer Token；
- 系统 Worker。

不要信任客户端自报 username。

### business ID

尽量记录：

```text
informationId
snapshotId
promptProfileId
promptVersionId
analysisId
batchId
recommendationRunId
```

## 4. Level

INFO：

- 正常业务关键节点；
- 状态流转；
- 完成；
- 合理汇总。

WARN：

- validation；
- business rejection；
- auth/CSRF；
- Budget Guard；
- invalid Envelope；
- no candidate；
- recommendation auto trigger skip。

ERROR：

- DB；
- Provider；
- unknown runtime；
- worker/system failure。

ERROR 保留 stack trace。

## 5. HTTP

```text
INFO HTTP request started, method=..., path=...
INFO HTTP request completed, method=..., path=..., status=..., durationMs=...
```

不要求完整 Request/Response Body。

## 6. Validation / Failure

前端收到重要 4xx / business failure 时，后端应尽量存在 WARN。

```text
WARN Request validation rejected,
path=...,
errorCode=...,
reason=...
```

避免同一异常多层重复 stack trace。

## 7. Prompt

```text
Prompt profile update requested, promptProfileId=...
Prompt profile updated, promptProfileId=..., promptVersionId=...
```

不打印 Prompt 正文。

## 8. Analysis / Provider

```text
Single analysis requested, snapshotId=...
AI analysis started, analysisId=..., provider=..., model=...
AI analysis completed, analysisId=..., durationMs=..., inputTokens=..., outputTokens=...
AI analysis failed, analysisId=..., durationMs=..., reason=...
```

不打印完整 Provider Payload。

## 9. Batch

```text
Analysis batch created, batchId=..., triggerType=...
Analysis batch started, batchId=..., candidateCount=...
Analysis batch progress, ...
Analysis batch completed, batchId=..., status=..., durationMs=...
```

Progress 不逐条 INFO。

## 10. Collector / Archive

```text
Collector submission received, source=BOSS, itemCount=...
Collector submission validation passed, ...
Information archive completed, received=..., inserted=..., updated=..., unchanged=..., snapshotsCreated=..., durationMs=...
Collector submission rejected, reason=...
Information archive failed, ...
```

## 11. Recommendation

```text
Recommendation trigger evaluating, sourceAnalysisBatchId=...
Recommendation trigger skipped, reason=...
Recommendation run created, recommendationRunId=..., triggerType=...
Recommendation run started, ...
Recommendation candidates resolved, candidateCount=..., eligibleCount=...
Recommendation scoring completed, durationMs=...
Recommendation run completed, resultCount=..., durationMs=...
Recommendation run failed, durationMs=..., reason=...
```

## 12. Interaction / BOSS Contact Status

```text
Recommendation interaction viewed, informationId=...
Recommendation feedback updated, informationId=..., feedbackState=...
Job disposition updated, informationId=..., jobDisposition=CONTACTED
Job disposition updated, informationId=..., jobDisposition=CONTACTED_NOT_SUITABLE
```

只记录状态，不记录聊天内容、沟通详情。

## 13. duration

统一：

```text
durationMs
```

## 14. Async MDC

用户请求触发异步任务时尽量传播 MDC。

后台系统线程无用户上下文不伪造 username。

后台链路优先依赖：

```text
batchId
analysisId
recommendationRunId
```

## 15. 敏感内容边界

本阶段不建设复杂脱敏框架。

继续禁止：

- Authorization；
- Token；
- Cookie；
- API Key；
- 密码；
- DB Secret；
- 完整 Provider Raw Response；
- 完整 Collector Raw Payload。

也不要求打印：

- Prompt 正文；
- 完整 AI input/output；
- BOSS 聊天内容；
- 完整 HTTP body。

## 16. Codex Checklist

每个后端 TASK 完成前检查：

- [ ] 关键入口 INFO；
- [ ] 关键完成节点 INFO；
- [ ] 用户可见失败 WARN；
- [ ] 未知异常 ERROR + stack trace；
- [ ] 长流程 durationMs；
- [ ] 有用户时 username；
- [ ] HTTP requestId；
- [ ] Async 不串 MDC；
- [ ] 不逐条刷无意义 INFO；
- [ ] 不机械 method enter/exit；
- [ ] Interaction / disposition 变更有节点日志；
- [ ] 不存在新增“前端失败、后端完全无定位日志”的路径。

## 17. AGENTS

`backend/information-hub/AGENTS.md` 必须引用本文件，并要求所有新增/修改后端业务代码遵守。
