# Recommendation Refresh V1

状态：Accepted

实施状态：Manual Refresh 与 Run list/detail 已由 TASK-040 落地；Auto Trigger 已由 TASK-041 落地；TASK-044 已完成全栈验收。

## Manual Refresh

```http
POST /api/v1/recommendations/{informationType}/refresh
```

Session + CSRF。

Phase 4 当前只接受 `informationType = JOB`。

成功：

```http
202 Accepted
```

```json
{
  "runId": 301,
  "status": "PENDING"
}
```

## Preconditions

- Profile exists；
- bound Prompt Profile has Active Version；
- current user 没有冲突的 PENDING/RUNNING Manual Run。

冲突：

```text
409 RECOMMENDATION_RUN_IN_PROGRESS
```

## No AI

Manual Refresh 不调用：

- Preview；
- Analysis Batch；
- Provider。

## Run API

```http
GET /api/v1/recommendations/{informationType}/runs/{runId}
GET /api/v1/recommendations/{informationType}/runs?limit=20
```

Owner + Information Type isolated。Run 响应显式返回 `informationType`。

## Auto Trigger

内部：

```text
Analysis Batch COMPLETED / PARTIAL_FAILED
→ eligible
→ Auto Run
```

Eligibility：

```text
same user
same bound Prompt Profile
informationType = JOB
```

`FAILED/NOOP` 不创建。

同一 source batch 幂等。

## Run Failure

FAILED 不影响上一轮成功 Feed。

## TASK-040 Implementation Note

- Manual Refresh 使用 Profile 行锁串行化同一 Owner + Information Type 的并发请求；
- Worker 通过 `FOR UPDATE SKIP LOCKED` claim PENDING Run，并恢复超过配置阈值的 stale RUNNING Run；
- Run 冻结 Generic Profile Core、JOB Domain Extension、Prompt Active Version、算法身份与候选窗口；
- Candidate、Scoring、Ranking 在事务外本地执行，不调用 AI Provider；
- Recommendation Item 在最终短事务中一次性持久化，全部成功后 Run 才进入 `COMPLETED`，空结果进入 `NOOP`；
- TASK-040 未实现本文 Auto Trigger 段落；该范围现已由 TASK-041 落地。

## TASK-041 Implementation Note

- Analysis Batch 在终态事务内发布进程内事件，Recommendation Listener 严格在 `AFTER_COMMIT` 阶段处理；
- Auto Run 创建使用独立 `REQUIRES_NEW` 短事务，不改变已提交的 Analysis Batch 结果；
- `COMPLETED/PARTIAL_FAILED` 进入 Profile、Owner、Prompt Profile 与 JOB eligibility；`FAILED/NOOP` 稳定跳过；
- Manual/Scheduled Batch 共用触发链，Auto Run 冻结来源 Batch Prompt Version；
- 同一 `sourceAnalysisBatchId` 先查重并由数据库唯一键提供并发最终幂等保证；
- 未增加 Kafka、Outbox、Recommendation Cron 或新的数据库结构。
