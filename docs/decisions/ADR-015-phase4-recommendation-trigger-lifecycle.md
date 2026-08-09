# ADR-015：Recommendation 只有 Analysis Batch 完成与 Manual Refresh 两种业务触发

状态：Accepted

## 自动 Trigger

Analysis Batch：

```text
MANUAL / SCHEDULED
```

终态：

```text
COMPLETED / PARTIAL_FAILED
```

且：

```text
Profile exists
same Owner
same bound Prompt Profile
```

则创建：

```text
RecommendationRun(trigger=ANALYSIS_BATCH_COMPLETED)
```

Auto Run 使用来源 Batch Prompt Version。

`FAILED` / `NOOP` 不创建。

同一 `sourceAnalysisBatchId` 幂等。

## Manual

用户显式 Refresh：

- current Profile；
- bound Prompt Profile current Active Version；
- existing Analysis only；
- no Provider call。

## Worker

Worker 只消费 PENDING Run，不是新的业务 Trigger。

## 不采用

- independent recommendation cron；
- profile update trigger；
- collector trigger；
- feed GET trigger；
- single analysis trigger。

## Event

使用 Spring 进程内 AFTER_COMMIT Event。

Phase 4 不引入 Kafka/Outbox Recommendation Event。
