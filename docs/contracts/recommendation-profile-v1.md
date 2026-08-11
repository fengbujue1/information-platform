# Recommendation Profile V1

状态：Accepted

实施状态：TASK-035 已完成 Backend 与 API；TASK-043/044 已完成 Web 与全栈验收。

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

TASK-035 实际规范化规则：

- `null` 数组按空数组处理；
- 元素执行 `trim`，删除 `null`、空白和完全重复值；
- 规范化结果按 Unicode 字典序稳定排序；
- 每个数组最多 50 个有效元素；
- 每个元素最多 100 个 Unicode 字符；
- `preferredRemoteTypes` 规范为大写，只接受 `ONSITE`、`HYBRID`、`REMOTE`；
- `salaryMinMonthlyYuan` 允许为空，非空时不得为负数。

`analysisPromptProfileId`：

- 存在；
- 同 Owner；
- `analysisDefinitionKey = JOB_USER_RELEVANCE`；
- 状态为 `ACTIVE`；
- 已存在 Active Prompt Version。

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

## Implementation Note

TASK-035 已实现：

```text
GET /api/v1/recommendation/profiles/JOB
PUT /api/v1/recommendation/profiles/JOB
```

- GET/PUT 都要求同源 Session；PUT 需要 CSRF；
- GET 不存在的 Owner Profile 返回 404；
- PUT 首次调用创建 Core + JOB Extension，后续调用完整替换；
- 响应为 Core + JOB Extension 的扁平组合 DTO，不返回 `userId`；
- `contentHash` 覆盖规范化后的完整组合 Profile，数组顺序或重复项变化不会改变 hash；
- PUT 不创建 Analysis、Recommendation Run 或刷新事件。
