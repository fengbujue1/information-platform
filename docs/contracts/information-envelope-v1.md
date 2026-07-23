# InformationEnvelope V1 接入协议

协议版本：1  
接口版本：v1  
状态：Accepted

## 1. 目的

所有 Collector 使用统一 InformationEnvelope 向 Information Hub 提交信息。

Phase 1 支持：

```text
informationType = JOB
source = BOSS
```

BOSS 详细映射见：

```text
docs/contracts/boss-job-field-mapping.md
```

## 2. 接口

```http
POST /api/v1/collector/items
Content-Type: application/json
Authorization: Bearer YOUR_COLLECTOR_TOKEN
```

Phase 1 暂不实现批量接口。

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
  "collectedAt": "2026-07-22T15:14:09.656Z",
  "collector": {
    "collectorId": "boss-collector-desktop",
    "collectorVersion": "2.1.0"
  },
  "collectionContext": {
    "runId": "36b2cd8e-f880-4418-a34c-833ecbe8e628",
    "keyword": "java",
    "city": "成都",
    "queryFilters": {},
    "filterDescriptions": [],
    "resultTotal": 90,
    "sourceScrapedAt": "2026-07-22T23:14:09.656+08:00",
    "timeZoneAssumption": "Asia/Shanghai"
  },
  "extension": {
    "sourceCompanyId": "sample-encrypt-brand-id",
    "sourceRecruiterId": "sample-encrypt-boss-id",
    "companyName": "示例科技有限公司",
    "companyUrl": "https://www.zhipin.com/gongsi/sample-encrypt-brand-id.html",
    "companyScaleText": "100-499人",
    "companyStageText": "B轮",
    "companyIndustryText": "互联网",
    "salaryText": "20-35K·13薪",
    "salarySource": "API",
    "salaryMinMonthlyYuan": 20000,
    "salaryMaxMonthlyYuan": 35000,
    "salaryMonths": 13,
    "locationName": "成都·武侯区·中和",
    "cityName": "成都",
    "areaName": "武侯区",
    "businessDistrictName": "中和",
    "experienceText": "3-5年",
    "educationText": "本科",
    "recruiterName": null,
    "recruiterTitle": "招聘经理",
    "recruiterActiveText": null,
    "remoteType": "UNKNOWN",
    "jobStatus": "ACTIVE",
    "detailStatus": "FETCHED",
    "detailCollectedAt": null,
    "sourceTags": [
      "3-5年",
      "本科"
    ],
    "sourceSkillTags": [
      "Java",
      "Spring Boot",
      "MySQL"
    ],
    "welfare": [
      "五险一金",
      "带薪年假"
    ]
  },
  "rawPayload": {
    "list": {
      "job_id": "sanitized-local-join-id",
      "title": "Java后端开发工程师",
      "salary": "20-35K·13薪",
      "salary_source": "api",
      "location": "成都·武侯区·中和",
      "tags": "3-5年 | 本科",
      "boss_name": "示例科技有限公司",
      "boss_title": "招聘经理",
      "encrypt_job_id": "sample-encrypt-job-id",
      "encrypt_boss_id": "sample-encrypt-boss-id",
      "encrypt_brand_id": "sample-encrypt-brand-id"
    },
    "detail": {
      "job_id": "sanitized-local-join-id",
      "jd": "脱敏后的职位描述",
      "skill_tags": []
    }
  }
}
```

## 4. 顶层字段

| 字段 | 类型 | 必填 | 限制 | 说明 |
|---|---|---:|---|---|
| `schemaVersion` | integer | 是 | 当前固定为 1 | 协议版本 |
| `informationType` | string | 是 | 1～32 字符 | 当前为 JOB |
| `source` | string | 是 | 1～64 字符 | 当前为 BOSS |
| `sourceItemId` | string | 是 | 1～255 字符 | BOSS 使用 encrypt_job_id |
| `sourceUrl` | string | 否 | 最大 2048 字符 | 来源页面地址 |
| `title` | string | 是 | 1～1000 字符 | 标题 |
| `content` | string/null | 否 | 服务端限制请求体总大小 | 正文或 JD |
| `publishTime` | datetime/null | 否 | ISO-8601 | 来源真实发布时间 |
| `collectedAt` | datetime | 是 | ISO-8601 带偏移 | 采集时间 |
| `collector` | object | 是 | 见下文 | Collector 信息 |
| `collectionContext` | object | 否 | 见下文 | 脱敏采集上下文 |
| `extension` | object | 是 | JOB 时符合 JOB 结构 | 业务扩展 |
| `rawPayload` | object | 是 | JSON Object | 安全清理后的原始业务数据 |

## 5. collector

| 字段 | 类型 | 必填 | 限制 |
|---|---|---:|---|
| `collectorId` | string | 是 | 1～128 字符 |
| `collectorVersion` | string | 是 | 1～64 字符 |

## 6. collectionContext

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `runId` | string | 否 | 单次采集运行 ID |
| `keyword` | string | 否 | 搜索关键词 |
| `city` | string | 否 | 搜索城市 |
| `queryFilters` | object | 否 | 脱敏筛选条件 |
| `filterDescriptions` | array[string] | 否 | 筛选说明 |
| `resultTotal` | integer | 否 | 列表总数 |
| `sourceScrapedAt` | datetime/string | 否 | 来源输出中的采集时间 |
| `timeZoneAssumption` | string | 否 | 旧无时区数据的解释假设 |

## 7. JOB extension

| 字段 | 类型 | 必填 |
|---|---|---:|
| `sourceCompanyId` | string/null | 否 |
| `sourceRecruiterId` | string/null | 否 |
| `companyName` | string/null | 否 |
| `companyUrl` | string/null | 否 |
| `companyScaleText` | string/null | 否 |
| `companyStageText` | string/null | 否 |
| `companyIndustryText` | string/null | 否 |
| `salaryText` | string/null | 否 |
| `salarySource` | string/null | 否 |
| `salaryMinMonthlyYuan` | integer/null | 否 |
| `salaryMaxMonthlyYuan` | integer/null | 否 |
| `salaryMonths` | integer/null | 否 |
| `locationName` | string/null | 否 |
| `cityName` | string/null | 否 |
| `areaName` | string/null | 否 |
| `businessDistrictName` | string/null | 否 |
| `experienceText` | string/null | 否 |
| `educationText` | string/null | 否 |
| `recruiterName` | string/null | 否 |
| `recruiterTitle` | string/null | 否 |
| `recruiterActiveText` | string/null | 否 |
| `remoteType` | string | 否 |
| `jobStatus` | string | 否 |
| `detailStatus` | string | 否 |
| `detailCollectedAt` | datetime/null | 否 |
| `sourceTags` | array[string]/null | 否 |
| `sourceSkillTags` | array[string]/null | 否 |
| `welfare` | array[string]/null | 否 |

默认值建议：

```text
remoteType = UNKNOWN
jobStatus = ACTIVE（职位来自本次有效搜索结果时）
detailStatus = UNKNOWN
```

`detailStatus` 仅支持：

```text
UNKNOWN
FETCHED
FAILED
UNAVAILABLE
```

## 8. 数组拆分

对于 `|` 分隔字符串：

1. 以 `|` 分割。
2. 去除首尾空白。
3. 删除空字符串。
4. 保持首次出现顺序。
5. 删除完全重复项。
6. 原始字符串保留在 rawPayload。

`tags` 使用规则识别经验和学历，不依赖固定位置。无法识别时，对应 `experienceText` 或 `educationText` 保持 null，仅在 `sourceTags` 中保留来源标签。

## 9. 服务端维护字段

Collector 不得提交：

- `id`
- `firstSeenTime`
- `lastSeenTime`
- `contentHash`
- `currentVersionNo`
- `createdAt`
- `updatedAt`
- `analysisStatus`
- `recommendationScore`

## 10. 幂等与非破坏性更新

幂等键：

```text
source + informationType + sourceItemId
```

重复提交时：

- 返回相同 informationId。
- 缺失、null、空字符串不覆盖已有有效值。
- 空数组不覆盖已有非空数组。
- 合并后计算 contentHash。
- Hash 变化时新增快照。
- V1 不支持主动清空字段。

## 11. rawPayload 安全

以下字段必须在提交 Information Hub 前删除，`rawPayload` 不得包含：

- Cookie
- Authorization Header
- 登录 Token
- Chrome Profile
- 账号密码
- 浏览器本地凭证
- `security_id`
- `lid`

## 12. 成功响应

首次创建：

```json
{
  "success": true,
  "code": "ITEM_CREATED",
  "data": {
    "informationId": 1001,
    "created": true,
    "contentChanged": true,
    "versionNo": 1,
    "snapshotCreated": true
  }
}
```

幂等更新：

```json
{
  "success": true,
  "code": "ITEM_UPDATED",
  "data": {
    "informationId": 1001,
    "created": false,
    "contentChanged": false,
    "versionNo": 1,
    "snapshotCreated": false
  }
}
```

## 13. 兼容性

- 新增可选字段属于兼容变更。
- 删除字段、修改含义、可选改必填属于不兼容变更。
- 不兼容变更必须升级 schemaVersion。
