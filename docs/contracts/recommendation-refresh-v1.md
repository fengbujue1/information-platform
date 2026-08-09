# Recommendation Refresh V1

状态：Accepted

## Manual Refresh

```http
POST /api/v1/recommendations/refresh
```

Session + CSRF。

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
GET /api/v1/recommendations/runs/{runId}
GET /api/v1/recommendations/runs?limit=20
```

Owner isolated。

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
```

`FAILED/NOOP` 不创建。

同一 source batch 幂等。

## Run Failure

FAILED 不影响上一轮成功 Feed。
