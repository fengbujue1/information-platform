# InformationEnvelope V1 接入协议

协议版本：1  
接口版本：v1  
当前状态：Draft

## 1. 目的

InformationEnvelope 是所有采集器向 Information Hub 提交信息时使用的统一数据结构。

当前支持：

```text
informationType = JOB
source = BOSS
```

未来可以扩展：

- `NEWS`
- `GOVERNMENT`
- `HOUSE_PRICE`
- `EDUCATION`
- `REPORT`
- `OTHER`

## 2. 接口

```http
POST /api/v1/collector/items
Content-Type: application/json
Authorization: Bearer <collector-token>
```

## 3. 请求结构

```json
{
  "schemaVersion": 1,
  "informationType": "JOB",
  "source": "BOSS",
  "sourceItemId": "sample-job-001",
  "sourceUrl": "https://www.zhipin.com/job_detail/example.html",
  "title": "Java后端开发工程师",
  "content": "岗位职责和任职要求……",
  "publishTime": null,
  "collectedAt": "2026-07-23T10:30:00+08:00",
  "collector": {
    "collectorId": "boss-collector-desktop",
    "collectorVersion": "1.0.0"
  },
  "extension": {
    "sourceCompanyId": "sample-company-001",
    "companyName": "示例科技有限公司",
    "salaryText": "20-35K",
    "locationName": "上海·浦东新区",
    "cityName": "上海",
    "areaName": "浦东新区",
    "experienceText": "3-5年",
    "educationText": "本科",
    "recruiterName": "示例招聘者",
    "recruiterTitle": "招聘经理",
    "recruiterActiveText": "今日活跃",
    "remoteType": "UNKNOWN",
    "jobStatus": "ACTIVE",
    "skills": ["Java", "Spring Boot", "MySQL"],
    "welfare": ["五险一金", "带薪年假"]
  },
  "rawPayload": {
    "sample": "这里保存脱敏后的原始 BOSS 数据"
  }
}
```

## 4. 顶层字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `schemaVersion` | integer | 是 | 协议版本，当前固定为 1 |
| `informationType` | string | 是 | 信息类型，当前支持 JOB |
| `source` | string | 是 | 数据来源，当前支持 BOSS |
| `sourceItemId` | string | 是 | 来源平台稳定唯一 ID |
| `sourceUrl` | string | 否 | 来源页面地址 |
| `title` | string | 是 | 信息标题 |
| `content` | string | 否 | 信息正文 |
| `publishTime` | ISO-8601 datetime | 否 | 来源真实发布时间 |
| `collectedAt` | ISO-8601 datetime | 是 | 采集时间 |
| `collector` | object | 是 | 采集器元数据 |
| `extension` | object | 是 | 业务扩展字段 |
| `rawPayload` | object | 是 | 原始采集数据 |

## 5. JOB extension 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `sourceCompanyId` | string | 否 | 来源平台公司 ID |
| `companyName` | string | 否 | 公司名称 |
| `salaryText` | string | 否 | 原始薪资文本 |
| `locationName` | string | 否 | 原始地点文本 |
| `cityName` | string | 否 | 城市 |
| `areaName` | string | 否 | 区县 |
| `experienceText` | string | 否 | 经验要求 |
| `educationText` | string | 否 | 学历要求 |
| `recruiterName` | string | 否 | 招聘者姓名 |
| `recruiterTitle` | string | 否 | 招聘者职位 |
| `recruiterActiveText` | string | 否 | 招聘者活跃状态原文 |
| `remoteType` | string | 否 | UNKNOWN、ONSITE、HYBRID、REMOTE |
| `jobStatus` | string | 否 | UNKNOWN、ACTIVE、OFFLINE |
| `skills` | array[string] | 否 | 技能列表 |
| `welfare` | array[string] | 否 | 福利列表 |

## 6. 服务端维护字段

采集器不得提交：

- `id`
- `firstSeenTime`
- `lastSeenTime`
- `contentHash`
- `createdAt`
- `updatedAt`
- `analysisStatus`
- `recommendationScore`

## 7. 时间规则

所有时间使用 ISO-8601，例如：

```text
2026-07-23T10:30:00+08:00
```

`publishTime` 只有来源网站明确提供真实发布时间时才填写；未知时为 `null`。

禁止：

```text
publishTime = collectedAt
```

## 8. 幂等规则

```text
source + informationType + sourceItemId
```

相同幂等键重复提交时：

- 不创建重复记录
- 更新 `lastSeenTime`
- 更新允许变化的字段
- 返回同一个 `informationId`

## 9. rawPayload 规则

不得包含：

- Cookie
- Authorization Header
- 登录 Token
- Chrome Profile
- 用户账号密码
- 浏览器本地存储凭证

## 10. 成功响应

```json
{
  "success": true,
  "code": "ITEM_CREATED",
  "message": "Information item created",
  "data": {
    "informationId": 1001,
    "created": true,
    "contentChanged": true
  }
}
```

## 11. 错误响应

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

## 12. HTTP 状态码

| 状态码 | 场景 |
|---:|---|
| 200 | 幂等更新成功 |
| 201 | 首次创建成功 |
| 400 | 参数错误 |
| 401 | 采集器未授权 |
| 422 | 数据结构合法但业务含义无法处理 |
| 500 | 服务端异常 |
| 503 | 服务暂时不可用 |

## 13. BOSS 字段映射

| BOSS 原始字段 | InformationEnvelope 字段 | 数据库字段 | 状态 |
|---|---|---|---|
| 待确认 | `sourceItemId` | `source_item_id` | 待分析 |
| 待确认 | `title` | `title` | 待分析 |
| 待确认 | `content` | `content` | 待分析 |
| 待确认 | `extension.companyName` | `company_name` | 待分析 |
| 待确认 | `extension.salaryText` | `salary_text` | 待分析 |
| 待确认 | `extension.recruiterActiveText` | `recruiter_active_text` | 待分析 |
