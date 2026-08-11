# BOSS 职位字段映射

状态：Accepted
实施状态：TASK-006/TASK-006B 已实现，Phase 1 端到端验收已完成。
接受日期：2026-07-29
数据样本：2026-07-22 成都 Java 搜索结果

## 1. 输入文件

列表文件：

```text
boss_jobs_20260722_2313.json
```

结构：

```text
搜索批次元数据
└── jobs[90]
```

详情文件：

```text
boss_details_20260722_2314(2).json
```

结构：

```text
details[25]
```

## 2. 合并键

Collector 内部使用：

```text
list.jobs[].job_id = details[].job_id
```

平台幂等 ID 使用：

```text
sourceItemId = list.jobs[].encrypt_job_id
```

禁止使用 `job_id` 替代 `encrypt_job_id`。

## 3. 批次字段映射

| 列表根字段 | collectionContext 字段 | 说明 |
|---|---|---|
| `keyword` | `keyword` | 搜索关键词 |
| `city` | `city` | 搜索城市 |
| `filters` | `queryFilters` | 筛选条件 |
| `filter_desc` | `filterDescriptions` | 筛选描述 |
| `total` | `resultTotal` | 本次列表数量 |
| `scraped_at` | `sourceScrapedAt` | 必须补充时区后提交 |
| 无 | `runId` | Mapper 生成，未来由 Collector 输出 |

## 4. 通用字段映射

| BOSS 字段 | InformationEnvelope | 数据库 |
|---|---|---|
| 固定值 | `informationType=JOB` | `information_item.information_type` |
| 固定值 | `source=BOSS` | `information_item.source` |
| `encrypt_job_id` | `sourceItemId` | `information_item.source_item_id` |
| `job_link` | `sourceUrl` | `information_item.source_url` |
| `title` | `title` | `information_item.title` |
| 详情 `jd` | `content` | `information_item.content` |
| 列表根 `scraped_at` | `collectedAt` | `information_item.collected_at` |
| 无 | `publishTime=null` | `information_item.publish_time` |

## 5. JOB extension 映射

| BOSS 字段 | extension 字段 | 数据库字段 |
|---|---|---|
| `encrypt_brand_id` | `sourceCompanyId` | `source_company_id` |
| `encrypt_boss_id` | `sourceRecruiterId` | `source_recruiter_id` |
| `boss_name` | `companyName` | `company_name` |
| `company_link` | `companyUrl` | `company_url` |
| `company_scale` | `companyScaleText` | `company_scale_text` |
| `company_stage` | `companyStageText` | `company_stage_text` |
| `company_industry` | `companyIndustryText` | `company_industry_text` |
| `salary` | `salaryText` | `salary_text` |
| `salary_source` | `salarySource` | `salary_source` |
| 薪资解析 | `salaryMinMonthlyYuan` | `salary_min_monthly_yuan` |
| 薪资解析 | `salaryMaxMonthlyYuan` | `salary_max_monthly_yuan` |
| 薪资解析 | `salaryMonths` | `salary_months` |
| `location` | `locationName` | `location_name` |
| 地点解析 | `cityName` | `city_name` |
| 地点解析 | `areaName` | `area_name` |
| 地点解析 | `businessDistrictName` | `business_district_name` |
| `tags` | `sourceTags` | `source_tags` |
| `tags` 解析 | `experienceText` | `experience_text` |
| `tags` 解析 | `educationText` | `education_text` |
| `boss_title` | `recruiterTitle` | `recruiter_title` |
| `boss_online_observed_at` | `recruiterActiveText` | `recruiter_active_text` |
| `skills` | `sourceSkillTags` | `source_skill_tags` |
| `welfare` | `welfare` | `welfare` |
| 详情是否存在 | `detailStatus` | `detail_status` |
| 详情时间 | `detailCollectedAt` | `detail_collected_at` |

## 6. 不建立标准列的字段

| 字段 | 处理 |
|---|---|
| `job_id` | Collector 内部 Join Key；安全 rawPayload 可保留 |
| `security_id` | 临时安全参数；提交 Information Hub 前删除 |
| `lid` | 请求追踪字段；提交 Information Hub 前删除 |
| `job_labels` | 当前样本与 tags 完全重复；只保留 rawPayload |
| `boss_online` | BOSS 瞬时在线布尔值；只保留 rawPayload，并通过观测时间建立标准映射 |
| 详情 `link` | 与 job_link 重复；只保留 rawPayload |
| 详情 `skill_tags` | 当前不是可靠技能；只保留 rawPayload |

## 7. 特殊含义

### boss_name

实际表示公司或品牌名：

```text
boss_name → companyName
```

不得映射为招聘者姓名。

### encrypt_boss_id

表示来源招聘者 ID：

```text
encrypt_boss_id → sourceRecruiterId
```

当前没有招聘者真实姓名字段：

```text
recruiterName = null
```

### sourceSkillTags

它是来源标签，不是平台标准化技能。包含技术词之外的工作方式、学历和经验标签。

### tags 识别

`tags` 使用规则识别经验和学历，不依赖固定位置。无法识别时，对应标准字段保持 null，仅在 `sourceTags` 中保留来源标签。

### boss_online

`boss_online` 来源于 BOSS 搜索响应的 `bossOnline`，只表示搜索响应到达时的瞬时状态。

当且仅当 `boss_online=true` 时，Collector 在响应处理时生成带明确时区的 `boss_online_observed_at`。TASK-006 Mapper 将该时间文本映射为：

```text
boss_online_observed_at → recruiterActiveText
```

false、缺失、null 或非法类型映射为 null。依靠 Information Hub 的非破坏性合并，null 不清空已有在线观测时间，新的非空时间覆盖旧值。

该字段表示“最近一次被本 Collector 观察到在线的时间”，不是 BOSS 官方最后活跃时间。`recruiterActiveText` 不参与 contentHash，仅该字段变化时不创建职位快照。

## 8. 详情状态

| 场景 | detailStatus |
|---|---|
| 匹配到详情且 jd 非空 | FETCHED |
| Collector 明确请求失败 | FAILED |
| 页面明确无详情或职位不可用 | UNAVAILABLE |
| 当前旧数据中无法判断 | UNKNOWN |

## 9. Mapper 校验

Mapper 至少校验：

1. 详情 `job_id` 必须能关联列表。
2. 重复字段冲突时记录警告。
3. `sourceItemId` 不能为空。
4. `boss_name` 不得写入 recruiterName。
5. 空字符串转换为 null。
6. security_id 和 lid 不发送到 Hub。
7. 列表和详情都保留在安全 rawPayload 中。
8. boss_online_observed_at 必须带明确时区，并且只在 boss_online=true 时映射。
9. Mapper 不得使用自身运行时间替代 Collector 在线观测时间。
