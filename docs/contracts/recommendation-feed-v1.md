# Recommendation Feed V1

状态：Accepted

实施状态：TASK-042 已实现 Backend Feed Query API；TASK-043 已实现 Phase 4 Web。

## API

```http
GET /api/v1/recommendations/{informationType}/feed?page=1&pageSize=20
```

Session Owner。

Phase 4 当前只接受 `informationType = JOB`。

默认：

```text
page=1
pageSize=20
max=100
```

## Base Feed

选择用户最新：

```text
RecommendationRun.informationType = requested informationType
AND RecommendationRun.status = COMPLETED
```

没有成功 Run 返回空 Feed，不实时计算。

## Current Interaction Visibility

在 current COMPLETED Run Item 上应用当前 Interaction：

```text
generic feedbackState == NOT_INTERESTED
→ hide

JOB extension jobDisposition == CONTACTED_NOT_SUITABLE
→ hide

JOB extension jobDisposition == CONTACTED
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
    "informationType": "JOB",
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

同 Owner、同 Information Type 的最新 Run 为 PENDING/RUNNING/FAILED 时，Feed 仍基于该 Information Type 最近的 COMPLETED Run。

## Profile stale

current profile hash != run hash：

```text
profileChangedSinceRun=true
```

只提示，不自动 Refresh。

## TASK-042 Implementation Note

- Endpoint 已按本 Contract 提供 Session Owner 隔离、默认/上限分页和仅 JOB 校验；
- Feed 通过最近 `COMPLETED` Run 读取持久化 Item，PENDING/RUNNING/FAILED 不覆盖旧成功 Run，没有成功 Run 时返回空 Feed；
- 当前 Interaction visibility 在数据库查询中先于 count/pagination 应用，恢复 NONE 后可重新显示当前 Run Item；
- Response 返回 score breakdown、reasons、current feedback/disposition/viewed、JOB 轻量展示字段与 Profile stale 标志；
- GET 不触发 Recommendation Run、Worker、Candidate、Scoring、AI Provider 或 Profile 更新。
