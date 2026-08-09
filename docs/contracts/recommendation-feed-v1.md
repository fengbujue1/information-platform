# Recommendation Feed V1

状态：Accepted

## API

```http
GET /api/v1/recommendations/feed?page=1&pageSize=20
```

Session Owner。

默认：

```text
page=1
pageSize=20
max=100
```

## Base Feed

选择用户最新：

```text
RecommendationRun.status = COMPLETED
```

没有成功 Run 返回空 Feed，不实时计算。

## Current Interaction Visibility

在 current COMPLETED Run Item 上应用当前 Interaction：

```text
feedbackState == NOT_INTERESTED
→ hide

jobDisposition == CONTACTED_NOT_SUITABLE
→ hide

jobDisposition == CONTACTED
→ keep visible
```

因此用户设置 hard exclusion 后，当前 Feed 立即消失，不等待下一次 Run。

RecommendationItem 历史不删除。

如果用户把状态恢复 NONE，且 Item 仍属于 current COMPLETED Run，可以重新显示。

## Response

```json
{
  "run": {
    "id": 301,
    "triggerType": "ANALYSIS_BATCH_COMPLETED",
    "completedAt": "2026-08-07T08:20:00Z",
    "algorithmKey": "JOB_RECOMMENDATION",
    "algorithmVersion": 1,
    "profileChangedSinceRun": false
  },
  "page": 1,
  "pageSize": 20,
  "total": 49,
  "items": [
    {
      "recommendationItemId": 1001,
      "informationId": 88,
      "snapshotId": 102,
      "rank": 1,
      "finalScore": 92.400,
      "scoreBreakdown": {
        "aiRelevanceScore": 95,
        "profileMatchScore": 86,
        "freshnessScore": 90
      },
      "reasons": [
        "AI 相关度高",
        "符合目标岗位偏好",
        "支持远程办公",
        "最近进入平台"
      ],
      "feedbackState": "INTERESTED",
      "jobDisposition": "CONTACTED",
      "viewed": true,
      "job": {
        "title": "Java Backend Engineer",
        "companyName": "Example",
        "salaryText": "20-35K",
        "locationName": "成都",
        "remoteType": "REMOTE",
        "sourceUrl": "..."
      }
    }
  ]
}
```

Job display fields 优先复用现有 Job Query DTO 语义。

## Running / Failed

最新 Run PENDING/RUNNING/FAILED 时，Feed 仍基于最近 COMPLETED Run。

## Profile stale

current profile hash != run hash：

```text
profileChangedSinceRun=true
```

只提示，不自动 Refresh。
