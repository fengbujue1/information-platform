# InformationEnvelope V1 接入协议

协议版本：1  
接口版本：v1  
状态：Draft，等待 TASK-001 和 TASK-002 最终确认。

## 1. 目的

所有 Collector 使用统一的 InformationEnvelope 向 Information Hub 提交信息。

Phase 1 支持：

```text
informationType = JOB
source = BOSS
```

## 2. 接口

```http
POST /api/v1/collector/items
Content-Type: application/json
Authorization: Bearer YOUR_COLLECTOR_TOKEN
```

Phase 1 暂不实现批量接口。

建议服务端将请求体上限设置为 5 MiB。超过限制时返回 HTTP 413。

## 3. 请求示例

```json
{
  "schemaVersion": 1,
  "informationType": "JOB",
  "source": "BOSS",
  "sourceItemId": "sample-encrypt-job-id",
  "sourceUrl": "https://www.zhipin.com/job_detail/sample-encrypt-job-id.html",
  "title": "Java后端开发工程师",
  "content": "脱敏后的职位描述",
  "publishTime": null,
  "collectedAt": "2026-07-23T02:30:00Z",
  "collector": {
    "collectorId": "boss-collector-desktop",
    "collectorVersion": "2.1.0"
  },
  "collectionContext": {
    "runId": "05e07af7-649d-49e0-a4dc-d1d821f70b25",
    "keyword": "Java",
    "city": "全国",
    "page": 1,
    "queryFilters": {
      "experience": "3-5年"
    }
  },
  "extension": {
    "sourceCompanyId": "sample-encrypt-brand-id",
    "companyName": "示例科技有限公司",
    "companyUrl": "https://www.zhipin.com/gongsi/sample-encrypt-brand-id.html",
    "companyScaleText": "100-499人",
    "companyStageText": "B轮",
    "companyIndustryText": "互联网",
    "salaryText": "20-35K·13薪",
    "locationName": "上海·浦东新区·张江",
    "cityName": "上海",
    "areaName": "浦东新区",
    "businessDistrictName": "张江",
    "experienceText": "3-5年",
    "educationText": "本科",
    "recruiterName": null,
    "recruiterTitle": "招聘经理",
    "recruiterActiveText": null,
    "remoteType": "UNKNOWN",
    "jobStatus": "ACTIVE",
    "jobLabels": [
      "五险一金"
    ],
    "skills": [
      "Java",
      "Spring Boot",
      "MySQL"
    ],
    "welfare": [
      "带薪年假"
    ]
  },
  "rawPayload": {
    "title": "Java后端开发工程师",
    "salary": "20-35K·13薪",
    "location": "上海·浦东新区·张江",
    "tags": "3-5年 | 本科",
    "boss_name": "示例科技有限公司",
    "boss_title": "招聘经理",
    "encrypt_job_id": "sample-encrypt-job-id"
  }
}
```

## 4. 顶层字段

| 字段 | 类型 | 必填 | 限制 | 说明 |
|---|---|---:|---|---|
| `schemaVersion` | integer | 是 | 当前必须为 1 | 协议版本 |
| `informationType` | string | 是 | 1～32 字符 | 当前为 JOB |
| `source` | string | 是 | 1～64 字符 | 当前为 BOSS |
| `sourceItemId` | string | 是 | 1～255 字符 | BOSS 使用 `encrypt_job_id` |
| `sourceUrl` | string | 否 | 最大 2048 字符 | 来源职位链接 |
| `title` | string | 是 | 1～1000 字符 | 职位标题 |
| `content` | string/null | 否 | 服务端正文上限另行配置 | 已校验的 JD |
| `publishTime` | datetime/null | 否 | ISO-8601 | 来源真实发布时间 |
| `collectedAt` | datetime | 是 | ISO-8601 | 采集时间 |
| `collector` | object | 是 | 见下文 | 采集器信息 |
| `collectionContext` | object | 否 | 见下文 | 采集批次上下文 |
| `extension` | object | 是 | JOB 类型时符合 JOB 结构 | 业务扩展字段 |
| `rawPayload` | object | 是 | 必须为 JSON 对象 | 原始业务对象 |

空字符串经过 `trim` 后视为没有有效值。

## 5. collector 字段

| 字段 | 类型 | 必填 | 限制 | 说明 |
|---|---|---:|---|---|
| `collectorId` | string | 是 | 1～128 字符 | 实例标识，不含个人敏感信息 |
| `collectorVersion` | string | 是 | 1～64 字符 | 采集器版本 |

## 6. collectionContext 字段

`collectionContext` 用于排查采集批次，不参与信息幂等键和业务内容 Hash。

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `runId` | string | 否 | 单次采集运行标识，建议 UUID |
| `keyword` | string | 否 | 本次搜索关键词 |
| `city` | string | 否 | 本次搜索城市 |
| `page` | integer | 否 | 来源页码，必须大于 0 |
| `queryFilters` | object | 否 | 脱敏后的筛选条件 |

不得在 `queryFilters` 中放 Cookie、Token 或浏览器指纹。

## 7. JOB extension 字段

| 字段 | 类型 | 必填 | 当前 BOSS 映射 |
|---|---|---:|---|
| `sourceCompanyId` | string | 否 | `encrypt_brand_id` |
| `companyName` | string | 否 | 原始字段 `boss_name`，实际来源是 `brandName` |
| `companyUrl` | string | 否 | `company_link` |
| `companyScaleText` | string | 否 | `company_scale` |
| `companyStageText` | string | 否 | `company_stage` |
| `companyIndustryText` | string | 否 | `company_industry` |
| `salaryText` | string | 否 | `salary` |
| `locationName` | string | 否 | `location` |
| `cityName` | string | 否 | 从 location 或独立字段解析 |
| `areaName` | string | 否 | 从 location 或独立字段解析 |
| `businessDistrictName` | string | 否 | 从 location 或独立字段解析 |
| `experienceText` | string | 否 | 当前从 `tags` 解析 |
| `educationText` | string | 否 | 当前从 `tags` 解析 |
| `recruiterName` | string/null | 否 | 当前无稳定结构化来源 |
| `recruiterTitle` | string | 否 | `boss_title` |
| `recruiterActiveText` | string/null | 否 | 当前无稳定结构化来源 |
| `remoteType` | string | 否 | UNKNOWN、ONSITE、HYBRID、REMOTE |
| `jobStatus` | string | 否 | UNKNOWN、ACTIVE、OFFLINE |
| `jobLabels` | array[string] | 否 | 拆分 `job_labels` |
| `skills` | array[string] | 否 | 拆分 `skills` |
| `welfare` | array[string] | 否 | 拆分 `welfare` |

禁止将 `boss_name` 映射为 `recruiterName`。

## 8. 字符串列表拆分规则

当前 Collector 使用 ` | ` 拼接列表。

Mapper 必须：

1. 以 `|` 分割。
2. 去除每项首尾空白。
3. 删除空字符串。
4. 保持原顺序。
5. 删除完全重复项。
6. 在 `rawPayload` 中保留原始字符串。

## 9. 时间规则

1. 接口接收 ISO-8601 带偏移时间。
2. Backend 将时间转换为 UTC。
3. `publishTime` 未知时为 `null`。
4. 禁止使用 `collectedAt` 填充 `publishTime`。

## 10. 服务端维护字段

Collector 不得提交：

- `id`
- `firstSeenTime`
- `lastSeenTime`
- `contentHash`
- `createdAt`
- `updatedAt`
- `analysisStatus`
- `recommendationScore`

## 11. 幂等规则

幂等键：

```text
source + informationType + sourceItemId
```

重复提交必须返回相同的 `informationId`。

## 12. 非破坏性更新规则

同一幂等键重复提交时，采用非破坏性合并。

1. 请求字段包含有效值时，使用新值。
2. 字段缺失、值为 `null`，或者字符串经过 `trim` 后为空时，默认保留数据库已有有效值。
3. 空数组默认视为没有新信息，不覆盖已有非空数组。
4. `rawPayload`、`collectedAt`、`collectorId`、`collectorVersion` 和可用的 `collectionContext` 更新为最近一次接收值。
5. `lastSeenTime` 每次成功接入都更新。
6. 合并完成后，对最终业务内容计算 `contentHash`。
7. V1 普通接入请求不支持主动清空字段。未来需要清空语义时，应使用专门字段或新协议版本。

示例：

```text
数据库已有完整 content
+ 本次详情抓取失败，content = null
= 保留数据库原 content
```

## 13. 状态语义

### information_item.status

平台内部生命周期：

- `ACTIVE`：平台当前正常保存和展示。
- `ARCHIVED`：平台内部归档，不代表来源职位失效。
- `DELETED`：逻辑删除；Phase 1 暂不实现。
- `UNKNOWN`：状态无法确定。

### job_information.job_status

来源职位业务状态：

- `ACTIVE`：来源明确显示职位有效。
- `OFFLINE`：来源明确返回职位失效。
- `UNKNOWN`：无法确定。

一次搜索未出现某职位，不能直接将其标记为 `OFFLINE`。

## 14. rawPayload 安全规则

不得包含：

- Cookie
- Authorization Header
- 登录 Token
- Chrome Profile
- 账号密码
- 浏览器本地凭证

## 15. 成功响应

首次创建使用 HTTP 201：

```json
{
  "success": true,
  "code": "ITEM_CREATED",
  "message": "Information item created",
  "data": {
    "informationId": 1001,
    "created": true,
    "contentChanged": true,
    "snapshotCreated": true
  }
}
```

幂等更新使用 HTTP 200：

```json
{
  "success": true,
  "code": "ITEM_UPDATED",
  "message": "Information item updated",
  "data": {
    "informationId": 1001,
    "created": false,
    "contentChanged": false,
    "snapshotCreated": false
  }
}
```

## 16. 错误响应

参数错误：

```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "sourceItemId must not be blank",
  "details": [
    {
      "field": "sourceItemId",
      "reason": "must not be blank"
    }
  ]
}
```

不支持的协议版本：

```json
{
  "success": false,
  "code": "UNSUPPORTED_SCHEMA_VERSION",
  "message": "schemaVersion 2 is not supported"
}
```

不支持的信息类型：

```json
{
  "success": false,
  "code": "UNSUPPORTED_INFORMATION_TYPE",
  "message": "informationType NEWS is not supported in Phase 1"
}
```

未授权：

```json
{
  "success": false,
  "code": "COLLECTOR_UNAUTHORIZED",
  "message": "Invalid collector token"
}
```

服务暂时不可用：

```json
{
  "success": false,
  "code": "SERVICE_UNAVAILABLE",
  "message": "Information Hub is temporarily unavailable"
}
```

## 17. HTTP 状态码

| 状态码 | 错误码或场景 |
|---:|---|
| 200 | 幂等更新成功 |
| 201 | 首次创建成功 |
| 400 | `INVALID_REQUEST` |
| 401 | `COLLECTOR_UNAUTHORIZED` |
| 413 | `PAYLOAD_TOO_LARGE` |
| 422 | `UNSUPPORTED_SCHEMA_VERSION`、`UNSUPPORTED_INFORMATION_TYPE` |
| 500 | `INTERNAL_ERROR` |
| 503 | `SERVICE_UNAVAILABLE` |

## 18. 兼容性

- 新增可选字段属于兼容变更。
- 删除字段、修改字段含义、可选改必填属于不兼容变更。
- 不兼容变更必须升级 `schemaVersion`。
