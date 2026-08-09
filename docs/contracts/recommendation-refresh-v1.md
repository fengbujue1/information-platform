# Recommendation Refresh V1

状态：Accepted

实施状态：Manual Refresh 与 Run list/detail 已由 TASK-040 落地；Auto Trigger 留待 TASK-041。

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
- TASK-040 未实现本文 Auto Trigger 段落，该范围仍属于 TASK-041。
