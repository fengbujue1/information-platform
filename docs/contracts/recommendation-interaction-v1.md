# Recommendation Interaction V1

状态：Accepted

## 1. View

```http
POST /api/v1/recommendation/interactions/{informationId}/view
```

Session + CSRF。

可选：

```json
{
  "recommendationItemId": 1001
}
```

行为：

```text
viewCount += 1
lastViewedAt = now
```

## 2. Feedback

```http
PUT /api/v1/recommendation/interactions/{informationId}/feedback
```

```json
{
  "feedbackState": "INTERESTED",
  "recommendationItemId": 1001
}
```

允许：

```text
NONE
INTERESTED
NOT_INTERESTED
```

PUT 覆盖 current state。

### Recommendation effect

`NOT_INTERESTED`：

- current Feed 立即隐藏；
- future Candidate hard exclude。

`INTERESTED`：

- 保留；
- V1 不自动修改 Profile；
- 不触发 Refresh。

`NONE`：

- 取消 Feedback hard exclusion。

## 3. Job Disposition

```http
PUT /api/v1/recommendation/interactions/{informationId}/job-disposition
```

```json
{
  "jobDisposition": "CONTACTED",
  "recommendationItemId": 1001
}
```

允许：

```text
NONE
CONTACTED
CONTACTED_NOT_SUITABLE
```

### CONTACTED

表示：

> 用户已经通过原始职位链接在 BOSS 等来源网站联系过该岗位，当前仍可能继续沟通。

行为：

- current Feed 保留；
- Feed 返回 `CONTACTED`；
- future Candidate 不排除。

### CONTACTED_NOT_SUITABLE

表示：

> 用户联系/沟通后确认职位不合适。

行为：

- current Feed 立即隐藏；
- future Candidate hard exclude；
- 不删除历史 RecommendationItem。

### NONE

取消 Job Disposition hard exclusion。

如果当前成功 Run 中仍有该 Item，可以重新显示。

## 4. Feedback 与 Disposition 独立

Backend 不强制：

```text
CONTACTED_NOT_SUITABLE
=> NOT_INTERESTED
```

二者可独立保存。

前端可在 UX 上提供便捷组合操作，但必须调用明确 Contract，不得让 Backend 隐式篡改另一个状态。

## 5. Owner / Attribution

- userId 只来自 Session；
- Information 必须存在；
- recommendationItemId 若提供，必须属于当前用户，并对应同一 informationId。

## 6. BOSS Automation

Phase 4 不自动查询/同步 BOSS 聊天记录。

该状态由用户手动设置。

## 7. Logs

```text
Recommendation feedback updated, informationId=..., feedbackState=...
Job disposition updated, informationId=..., jobDisposition=...
```

不记录聊天内容。
