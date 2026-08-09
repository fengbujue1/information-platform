# Recommendation Profile V1

状态：Accepted

## API

```http
GET /api/v1/recommendation/profiles/{informationType}
PUT /api/v1/recommendation/profiles/{informationType}
```

Session；PUT 需要 CSRF。

不接受可信 userId。

Phase 4 当前只接受：

```text
informationType = JOB
```

## Body

```json
{
  "analysisPromptProfileId": 12,
  "windowDays": 7,
  "topN": 50,
  "targetRoles": ["Java 后端", "大数据开发"],
  "preferredSkills": ["Java", "Spring Boot", "Spark"],
  "preferredCities": ["成都"],
  "preferredRemoteTypes": ["REMOTE", "HYBRID"],
  "salaryMinMonthlyYuan": 15000,
  "excludedKeywords": ["纯销售"]
}
```

响应额外：

```text
id
informationType
contentHash
createdAt
updatedAt
```

## Rules

```text
windowDays: 1..30
topN: 1..100
```

数组必须规范化、去空、去重并有长度/数量限制。

`analysisPromptProfileId`：

- 存在；
- 同 Owner；
- 可用于 JOB USER_RELEVANCE。

## Core / JOB Extension

对外 Contract 返回组合后的 JOB Profile，不暴露内部拆表：

```text
user_recommendation_profile Core
+
job_recommendation_profile Extension
```

Core 按 `(userId, informationType)` 唯一。`contentHash` 覆盖 Core 与 JOB Extension 的完整 canonical representation。

## Update

PUT 完整替换当前 Owner + Information Type 的组合 Profile。

修改 Profile：

- 不 AI；
- 不 Recommendation Refresh；
- 下次 Auto/Manual Run 生效。

## Delete

V1 无 DELETE。
