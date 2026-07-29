# Job Query API V1

状态：Accepted
接受日期：2026-07-29

## 1. 范围

本契约定义 Phase 1 职位分页、当前详情和历史快照查询。

所有接口只返回标准化字段，不返回 `information_item.raw_payload` 或 `information_snapshot.raw_payload`。TASK-009 不新增读取认证或原始数据管理接口。

所有时间字段以 UTC ISO-8601 格式输出，例如：

```text
2026-07-28T08:00:00Z
```

## 2. 统一响应

成功响应：

```json
{
  "success": true,
  "code": "JOBS_FOUND",
  "data": {}
}
```

失败响应：

```json
{
  "success": false,
  "code": "INVALID_JOB_PAGE_SIZE",
  "message": "size must be between 1 and 100"
}
```

## 3. 职位分页

```http
GET /api/v1/jobs
```

查询参数：

| 参数 | 默认值 | 规则 |
| --- | --- | --- |
| `page` | `1` | 从 1 开始 |
| `size` | `20` | 1 至 100 |
| `keyword` | 空 | 字面匹配职位标题、公司名称或职位正文 |
| `company` | 空 | 公司名称字面包含匹配 |
| `city` | 空 | 城市名称精确匹配 |
| `salaryMin` | 空 | 非负整数，单位为人民币元/月 |
| `salaryMax` | 空 | 非负整数，单位为人民币元/月 |
| `source` | 空 | 信息来源精确匹配 |
| `jobStatus` | 空 | 来源职位状态精确匹配 |
| `remoteType` | 空 | 办公方式精确匹配 |
| `sortBy` | `firstSeenTime` | 排序字段白名单 |
| `sortDirection` | `desc` | `asc` 或 `desc` |

允许的 `sortBy`：

- `firstSeenTime`
- `lastSeenTime`
- `publishTime`
- `salaryMinMonthlyYuan`

薪资筛选采用区间相交语义：

```text
职位最高月薪 >= salaryMin
且
职位最低月薪 <= salaryMax
```

所有排序都追加同方向的 `id` 次排序，确保相同排序值下分页稳定。

分页响应示例：

```json
{
  "success": true,
  "code": "JOBS_FOUND",
  "data": {
    "page": 1,
    "size": 20,
    "total": 1,
    "totalPages": 1,
    "items": [
      {
        "id": 1001,
        "source": "BOSS",
        "sourceItemId": "source-job-id",
        "sourceUrl": "https://example.test/jobs/1001",
        "title": "Java developer",
        "companyName": "Example company",
        "salaryText": "20-30K",
        "salaryMinMonthlyYuan": 20000,
        "salaryMaxMonthlyYuan": 30000,
        "salaryMonths": 12,
        "locationName": "成都·武侯区",
        "cityName": "成都",
        "experienceText": "3-5年",
        "educationText": "本科",
        "remoteType": "UNKNOWN",
        "jobStatus": "ACTIVE",
        "publishTime": null,
        "firstSeenTime": "2026-07-28T08:00:00Z",
        "lastSeenTime": "2026-07-28T08:00:00Z",
        "currentVersionNo": 1
      }
    ]
  }
}
```

列表响应不包含职位正文、采集上下文或 `rawPayload`。

## 4. 当前职位详情

```http
GET /api/v1/jobs/{id}
```

`id` 是 `information_item.id`，必须为正整数。

详情包含：

- Information 标准字段和当前版本号。
- 当前职位正文。
- 公司、薪资、地点、经验、学历和招聘者标准字段。
- 职位状态、详情采集状态和标签数组。
- Collector 标识和版本。

详情不包含 `rawPayload` 和采集上下文。

职位不存在时返回：

```text
HTTP 404
code = JOB_NOT_FOUND
```

## 5. 职位历史快照

```http
GET /api/v1/jobs/{id}/snapshots
```

响应按 `createdAt DESC, versionNo DESC` 排序。每条记录包含：

- 快照 ID 和版本号。
- `contentHash`。
- 该版本标题和正文。
- 标准化业务 JSON。
- 采集时间、创建时间和 Collector 版本。

快照响应不包含快照 `rawPayload`。

## 6. 参数错误

| 错误码 | 含义 |
| --- | --- |
| `INVALID_JOB_PAGE` | 页码小于 1 |
| `INVALID_JOB_PAGE_SIZE` | 每页数量不在 1 至 100 之间 |
| `INVALID_JOB_SORT_FIELD` | 排序字段不在白名单 |
| `INVALID_JOB_SORT_DIRECTION` | 排序方向不是 asc 或 desc |
| `INVALID_JOB_SALARY` | 薪资小于 0 |
| `INVALID_JOB_SALARY_RANGE` | 最低薪资大于最高薪资 |
| `INVALID_JOB_FILTER` | 筛选文本超过长度限制 |
| `INVALID_JOB_ID` | 职位 ID 不是正整数 |
| `INVALID_REQUEST_PARAMETER` | 路径或查询参数类型错误 |
