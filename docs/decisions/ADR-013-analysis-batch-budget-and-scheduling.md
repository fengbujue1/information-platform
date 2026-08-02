# ADR-013：统一 Batch Engine、Token Budget 与每日 Schedule

状态：Proposed  
日期：2026-08-02

## 背景

用户需要两种触发方式：

1. 页面手动分析最近 N 天信息；
2. 页面配置每天几点自动分析。

二者参数和限制必须一致。

AI 批处理存在费用风险，不能用“分析全部”无约束执行。

## 决策

手动和定时分析共用一个 Batch Engine。

```text
MANUAL ──────┐
             ├→ Candidate Resolver → Limits → Batch → Worker
SCHEDULED ───┘
```

## Manual

```text
Preview
→ Confirm
→ Create Batch
```

Preview 不调用 AI。

## Schedule

默认：

```text
enabled = false
time = 02:00
timezone = user-selected IANA timezone
frequency = DAILY
```

Phase 3 不开放 Cron 表达式。

Schedule 不直接调用 Provider，只创建 Batch。

## 限制

必须同时支持：

- max window days；
- max candidates；
- max estimated token budget；
- max output tokens per call。

超出候选数或 Token Budget 的项目延期，并记录原因。

## Token

- Estimated Token：Preview/Guard 使用；
- Actual Token：Provider Usage；
- Estimated 绝不冒充 Actual；
- 失败 Analysis 也可能产生 Actual Token；
- 每个 Provider Request 单独保存 Invocation。

## Schedule Prompt

Schedule 保存 Prompt Profile。

执行时：

```text
resolve current active prompt version
→ freeze to batch
```

## Overlap

同一 Schedule 最多一个活动 Batch。

新触发时间到达但旧 Batch 仍运行：

```text
SKIP_CONCURRENT
```

## Misfire

服务器停机错过执行时间，Phase 3 默认不补跑。

避免恢复后一次性补跑多日任务造成大量费用。

## 幂等

同一：

```text
scheduleId + scheduledFor
```

最多创建一次运行。

## 基础设施

第一版：

```text
Spring + MySQL
```

不引入 Quartz、XXL-JOB、Kafka、RabbitMQ 或 Redis Queue。

## 重新评估

出现：

- 数万 Schedule；
- 多实例高可用调度；
- 复杂 Cron；
- 多 Worker 高吞吐；

再评估专业调度/消息基础设施。
