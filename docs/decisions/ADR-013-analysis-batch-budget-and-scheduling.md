# ADR-013：统一 Batch Engine、Token Budget 与每日 Schedule

状态：Accepted
接受日期：2026-08-03
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

Preview 不持久化，返回 10 分钟有效的 HMAC 签名 token；Confirm 重算相同绝对窗口与候选/Estimate 指纹。Manual Batch 以 `(userId, manualRequestId)` 幂等。

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

Dispatcher 每 30～60 秒扫描，允许 5 分钟 misfire grace；超过后跳过历史计划。重叠触发创建 NOOP Batch 并推进下次计划。

## 限制

必须同时支持：

- max window days；
- max candidates；
- max estimated token budget；
- max output tokens per call。

超出候选数或 Token Budget 的项目延期，并记录原因。

冻结值：

```text
defaults: windowDays=3, maxCandidates=20, maxEstimatedTokens=75000
hard caps: windowDays=14, maxCandidates=50, maxEstimatedTokens=200000
maxOutputTokens per call=1000
```

Estimate V1：

```text
ceil((ceil(UTF-8 bytes / 3) + 64) × 1.20) + maxOutputTokens
method = UTF8_BYTES_DIV3_MARGIN20_V1
```

## Token

- Estimated Token：Preview/Guard 使用；
- Actual Token：Provider Usage；
- Estimated 绝不冒充 Actual；
- 失败 Analysis 也可能产生 Actual Token；
- 每个 Provider Request 单独保存 Invocation。

Invocation 绑定 Batch Item，保证历史 Batch Usage 不受 Analysis 后续重试影响。

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

Worker 单并发，使用短事务和 `FOR UPDATE SKIP LOCKED` 领取；Provider HTTP 调用不持有数据库事务。结果不确定的调用不自动盲重试。

## 重新评估

出现：

- 数万 Schedule；
- 多实例高可用调度；
- 复杂 Cron；
- 多 Worker 高吞吐；

再评估专业调度/消息基础设施。
