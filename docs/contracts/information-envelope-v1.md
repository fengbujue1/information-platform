# InformationEnvelope V1 接入协议

协议版本：1  
接口版本：v1  
状态：Draft，等待 TASK-001 和 TASK-002 最终确认。

## 1. 目的

所有 Collector 使用统一 InformationEnvelope 向 Information Hub 提交信息。

Phase 1 支持：

```text
informationType = JOB
source = BOSS
```

## 2. 接口

```http
POST /api/v1/collector/items
Content-Type: application/json
Authorization: Bearer <collector-token>
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
  "collectedAt": "2026-07-23T02:30:00Z",
  "collector": {
    "collectorId": "boss-collector-desktop",
    "collectorVersion": "2.1.0"
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
    "jobLabels": ["五险一金"],
    "skills": ["Java", "Spring Boot", "MySQL"],
    "welfare": ["带薪年假"]
  },
  "rawPayload": {
    "title": "Java后端开发工程师",
    "salary": "20-35K·13薪",
    "salary_source": "api",
    "location": "上海·浦东新区·张江",
    "tags": "3-5年 | 本科",
    "boss_name": "示例科技有限公司",
    "boss_title": "招聘经理",
    "company_scale": "100-499人",
    "company_stage": "B轮",
    "company_industry": "互联网",
    "job_labels": "五险一金",
    "skills": "Java | Spring Boot | MySQL",
    "encrypt_job_id": "sample-encrypt-job-id",
    "encrypt_boss_id": "sample-encrypt-boss-id",
    "encrypt_brand_id": "sample-encrypt-brand-id",
    "job_link": "https://www.zhipin.com/job_detail/sample-encrypt-job-id.html",
    "company_link": "https://www.zhipin.com/gongsi/sample-encrypt-brand-id.html",
    "welfare": "带薪年假"
  }
}
```

## 4. 顶层字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `schemaVersion` | integer | 是 | 当前固定为 1 |
| `informationType` | string | 是 | 当前为 JOB |
| `source` | string | 是 | 当前为 BOSS |
| `sourceItemId` | string | 是 | BOSS 当前使用 `encrypt_job_id` |
| `sourceUrl` | string | 否 | 来源职位链接 |
| `title` | string | 是 | 职位标题 |
| `content` | string | 否 | 已校验的 JD；详情未抓到时可为空 |
| `publishTime` | datetime/null | 否 | 真实发布时间；当前 BOSS Collector 通常为 null |
| `collectedAt` | datetime | 是 | ISO-8601 带偏移时间 |
| `collector` | object | 是 | 采集器信息 |
| `extension` | object | 是 | JOB 扩展 |
| `rawPayload` | object | 是 | 完整原始业务对象 |

## 5. Collector 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `collectorId` | string | 是 | 实例标识，不含个人敏感信息 |
| `collectorVersion` | string | 是 | 采集器版本 |

## 6. JOB extension 字段

| 字段 | 类型 | 必填 | 当前 BOSS 映射 |
|---|---|---:|---|
| `sourceCompanyId` | string | 否 | `encrypt_brand_id` |
| `companyName` | string | 否 | 当前原始字段 `boss_name`，实际来源是 `brandName` |
| `companyUrl` | string | 否 | `company_link` |
| `companyScaleText` | string | 否 | `company_scale` |
| `companyStageText` | string | 否 | `company_stage` |
| `companyIndustryText` | string | 否 | `company_industry` |
| `salaryText` | string | 否 | `salary` |
| `locationName` | string | 否 | `location` |
| `cityName` | string | 否 | 从 location 或后续独立字段解析 |
| `areaName` | string | 否 | 从 location 或后续独立字段解析 |
| `businessDistrictName` | string | 否 | 从 location 或后续独立字段解析 |
| `experienceText` | string | 否 | 当前从 `tags` 解析 |
| `educationText` | string | 否 | 当前从 `tags` 解析 |
| `recruiterName` | string/null | 否 | 当前无稳定结构化来源 |
| `recruiterTitle` | string | 否 | `boss_title` |
| `recruiterActiveText` | string/null | 否 | 当前无稳定结构化来源 |
| `remoteType` | string | 否 | 默认 UNKNOWN |
| `jobStatus` | string | 否 | 默认 ACTIVE 或 UNKNOWN，由规则确认 |
| `jobLabels` | array[string] | 否 | 拆分 `job_labels` |
| `skills` | array[string] | 否 | 拆分 `skills` |
| `welfare` | array[string] | 否 | 拆分 `welfare` |

禁止将 `boss_name` 映射为 `recruiterName`。

## 7. 字符串列表拆分规则

当前 Collector 使用 ` | ` 拼接列表。

Mapper 处理规则：

1. 以 `|` 分割；
2. 去除每项首尾空白；
3. 删除空字符串；
4. 保持原顺序；
5. 去除完全重复项；
6. `rawPayload` 仍保留原始字符串。

## 8. 时间规则

- 接口接收 ISO-8601。
- Backend 转换为 UTC。
- `publishTime` 未知时为 `null`。
- 禁止 `publishTime = collectedAt`。

## 9. 服务端维护字段

Collector 不提交：

- id
- firstSeenTime
- lastSeenTime
- contentHash
- createdAt
- updatedAt
- analysisStatus
- recommendationScore

## 10. 幂等规则

```text
source + informationType + sourceItemId
```

重复提交返回相同 `informationId`。

## 11. rawPayload 安全规则

不得包含：

- Cookie
- Authorization Header
- 登录 Token
- Chrome Profile
- 账号密码
- 浏览器本地凭证

## 12. 响应

首次创建使用 HTTP 201：

```json
{
  "success": true,
  "code": "ITEM_CREATED",
  "data": {
    "informationId": 1001,
    "created": true,
    "contentChanged": true
  }
}
```

幂等更新使用 HTTP 200：

```json
{
  "success": true,
  "code": "ITEM_UPDATED",
  "data": {
    "informationId": 1001,
    "created": false,
    "contentChanged": false
  }
}
```

## 13. 兼容性

- 新增可选字段：兼容变更。
- 删除字段、改含义、可选改必填：不兼容变更。
- 不兼容变更必须升级 `schemaVersion`。
