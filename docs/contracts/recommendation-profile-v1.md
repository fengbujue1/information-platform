# Recommendation Profile V1

状态：Accepted

## API

```http
GET /api/v1/recommendation/profile
PUT /api/v1/recommendation/profile
```

Session；PUT 需要 CSRF。

不接受可信 userId。

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

## Update

PUT 完整替换 current Profile。

修改 Profile：

- 不 AI；
- 不 Recommendation Refresh；
- 下次 Auto/Manual Run 生效。

## Delete

V1 无 DELETE。
