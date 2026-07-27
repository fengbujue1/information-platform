# Information Platform 数据库设计

设计版本：1.3  
状态：Accepted
当前阶段：Phase 1  
数据库：MySQL 8.x  
字符集：utf8mb4  
时间存储：UTC

## 1. 设计目标

Phase 1 支持 BOSS 职位接入、当前版本查询和历史版本归档，但公共数据库模型不能绑定 BOSS。

Phase 1 创建：

- `information_item`
- `job_information`
- `information_snapshot`

后续通过新的 Flyway 迁移增加 AI、推荐、用户和通知相关表。

## 2. 实际输入数据事实

本设计基于以下实际输出：

```text
boss_jobs_20260722_2313.json
boss_details_20260722_2314(2).json
```

当前样本事实：

- 列表文件包含 90 条职位。
- 详情文件包含 25 条职位详情。
- 25 条详情均可通过 `job_id` 关联到列表职位。
- 详情中的标题、公司、薪资、地点、标签和职位链接与对应列表记录一致。
- 详情文件主要新增 `jd`。
- `details.skill_tags` 当前主要为空，少量值为 `kanzhun` 或 `直聘`，不属于标准技能。
- 列表根节点包含 `keyword`、`city`、`filters`、`filter_desc`、`scraped_at` 和 `total`。
- 当前 `scraped_at` 没有时区偏移。
- 当前详情文件没有独立 `scraped_at`、`run_id` 和详情抓取状态。

## 3. 核心原则

1. 通用字段进入 `information_item`。
2. 职位专有字段进入 `job_information`。
3. 首次版本和业务内容变化版本进入 `information_snapshot`。
4. 当前表保存最新有效版本。
5. 中央 `raw_payload` 保存经过安全清理的原始业务字段。
6. Cookie、Token、`security_id`、`lid`、浏览器凭证等安全或临时请求字段不得进入中央数据库。
7. 不为所有来源字段建立数据库列。
8. 经常筛选、排序、关联或统计的字段才标准化。
9. 来源发布时间未知时保持 `NULL`。
10. `first_seen_time` 和 `last_seen_time` 由服务端维护。
11. 所有结构变更通过 Flyway 管理。
12. 已在共享环境执行的迁移不得修改，只能新增迁移。
13. 数据库不使用 ENUM，枚举合法性由 Java 代码校验。
14. 数据库字段使用 `snake_case`，Java 字段使用 `camelCase`。

## 4. 职位身份与两份文件合并

### 4.1 平台来源职位 ID

平台幂等键：

```text
source + information_type + source_item_id
```

BOSS 映射：

```text
source = BOSS
information_type = JOB
source_item_id = encrypt_job_id
```

`encrypt_job_id` 是 BOSS 来源职位标识，必须保留原始字符，包括可能存在的 `~`。

### 4.2 采集器本地关联键

列表和详情文件使用：

```text
job_id
```

进行本地关联。

当前样本中，`job_id` 与职位链接存在稳定派生关系，但数据库设计不依赖该派生算法。它只作为 Collector 内部 Join Key，不作为平台幂等 ID，也不单独建立标准数据库列。

### 4.3 合并优先级

Mapper 先在 Collector 内部合并列表和详情，再提交 InformationEnvelope。

规则：

1. 列表对象是结构化职位字段的主要来源。
2. 详情对象主要补充 `jd`。
3. 重复字段用于一致性校验。
4. 重复字段发生冲突时：
   - 保留列表值作为当前标准化字段；
   - 在日志中记录警告；
   - 在 `raw_payload` 中保留两边原值；
   - 不静默覆盖。
5. 没有详情记录时，职位仍可接入，`content` 保持 `NULL`。

## 5. 非破坏性合并

相同幂等键重复接入时：

1. 新请求中的有效值覆盖旧值。
2. 字段缺失、值为 `NULL`，或字符串 `trim` 后为空时，不覆盖已有有效值。
3. 空数组默认不覆盖已有非空数组。
4. 最近一次安全清理后的 `raw_payload`、采集时间和 Collector 元数据正常更新。
5. 合并完成后计算 `content_hash`。
6. Hash 未变化时，不新增快照。
7. Hash 变化时，更新当前表并插入新快照。
8. V1 普通接入请求不支持主动清空字段。

示例：

```text
数据库已有完整 JD
+ 本次详情抓取失败，content = NULL
= 保留数据库已有 JD
```

## 6. contentHash 规则

算法：

```text
SHA-256
```

Hash 输入是合并完成后的 Canonical JSON。

参与 Hash 的字段：

- `source_url`
- `title`
- `content`
- `source_company_id`
- `source_recruiter_id`
- `company_name`
- `company_url`
- `company_scale_text`
- `company_stage_text`
- `company_industry_text`
- `salary_text`
- `salary_min_monthly_yuan`
- `salary_max_monthly_yuan`
- `salary_months`
- `location_name`
- `city_name`
- `area_name`
- `business_district_name`
- `experience_text`
- `education_text`
- `recruiter_name`
- `recruiter_title`
- `remote_type`
- `job_status`
- `source_tags`
- `source_skill_tags`
- `welfare`

不参与 Hash：

- `salary_source`
- `recruiter_active_text`
- `detail_status`
- `detail_collected_at`
- `collected_at`
- Collector ID 和版本
- `collection_context`
- `first_seen_time`
- `last_seen_time`
- `raw_payload`
- `security_id`
- `lid`

规范化规则：

1. 字符串去除首尾空白。
2. 换行统一为 `\n`。
3. 每行去除行尾空白。
4. JSON Object 按 Key 排序。
5. 数组在持久化时保留来源顺序；计算 Hash 时去重后排序，避免仅顺序变化产生新版本。
6. `NULL` 与字段缺失在 Hash 中使用统一表示。
7. 不对正文内部正常空格进行激进压缩。

## 7. 时间规则

1. API 接收 ISO-8601 带偏移时间。
2. Java 使用 `OffsetDateTime` 接收。
3. 写入 MySQL 前统一转换为 UTC。
4. MySQL 使用 `DATETIME(3)` 保存 UTC。
5. API 输出 UTC `Z` 或明确偏移量。
6. `publish_time` 未知时保持 `NULL`。
7. 不得使用 `collected_at` 伪造 `publish_time`。

当前历史列表文件的：

```text
2026-07-22T23:14:09.656461
```

没有时区。历史导入时必须由配置显式指定 Collector 时区，例如：

```text
Asia/Shanghai
```

并在 `collection_context` 中记录该时间解释假设。未来 Collector 输出必须直接包含 `+08:00` 或 `Z`。

## 8. information_item

用途：保存所有信息类型的公共字段和当前最新版本。

| 字段 | 类型 | 允许为空 | 说明 |
|---|---|---:|---|
| `id` | BIGINT UNSIGNED | 否 | 主键，自增 |
| `information_type` | VARCHAR(32) | 否 | JOB、NEWS 等 |
| `source` | VARCHAR(64) | 否 | BOSS、RSS 等 |
| `source_item_id` | VARCHAR(255) | 否 | 来源稳定唯一 ID；BOSS 使用 encrypt_job_id |
| `source_url` | VARCHAR(2048) | 是 | 来源页面地址 |
| `title` | VARCHAR(1000) | 否 | 标题 |
| `content` | LONGTEXT | 是 | 当前最新正文；BOSS 为详情 JD |
| `publish_time` | DATETIME(3) | 是 | 来源真实发布时间，UTC |
| `collected_at` | DATETIME(3) | 否 | 最近一次有效采集时间，UTC |
| `first_seen_time` | DATETIME(3) | 否 | 服务端首次发现时间，UTC |
| `last_seen_time` | DATETIME(3) | 否 | 服务端最近发现时间，UTC |
| `content_hash` | CHAR(64) | 否 | 当前最终业务内容 SHA-256 |
| `current_version_no` | INT UNSIGNED | 否 | 当前快照版本号，从 1 开始 |
| `raw_payload` | JSON | 否 | 最近一次安全清理后的列表和详情原始业务对象 |
| `schema_version` | INT UNSIGNED | 否 | 接入协议版本 |
| `collector_id` | VARCHAR(128) | 否 | 最近一次 Collector 实例标识 |
| `collector_version` | VARCHAR(64) | 否 | 最近一次 Collector 版本 |
| `collection_context` | JSON | 是 | 最近一次脱敏采集批次上下文 |
| `status` | VARCHAR(32) | 否 | ACTIVE、ARCHIVED、DELETED、UNKNOWN |
| `created_at` | DATETIME(3) | 否 | 创建时间，UTC |
| `updated_at` | DATETIME(3) | 否 | 更新时间，UTC |

约束与索引：

```text
UNIQUE (source, information_type, source_item_id)
INDEX (information_type, last_seen_time)
INDEX (source, last_seen_time)
INDEX (status, last_seen_time)
INDEX (publish_time)
```

`information_item.status` 表示平台内部生命周期，不表示 BOSS 职位是否失效。

默认值建议：

```text
current_version_no = 1
status = ACTIVE
```

## 9. job_information

用途：保存职位当前最新的标准化业务字段。

| 字段 | 类型 | 允许为空 | 实际来源或说明 |
|---|---|---:|---|
| `information_id` | BIGINT UNSIGNED | 否 | 主键，关联 information_item.id |
| `source_company_id` | VARCHAR(255) | 是 | encrypt_brand_id；空字符串转 NULL |
| `source_recruiter_id` | VARCHAR(255) | 是 | encrypt_boss_id |
| `company_name` | VARCHAR(255) | 是 | 列表 boss_name；详情 company 用于校验 |
| `company_url` | VARCHAR(2048) | 是 | company_link；空字符串转 NULL |
| `company_scale_text` | VARCHAR(100) | 是 | company_scale |
| `company_stage_text` | VARCHAR(100) | 是 | company_stage；空字符串转 NULL |
| `company_industry_text` | VARCHAR(255) | 是 | company_industry |
| `salary_text` | VARCHAR(100) | 是 | salary 原文 |
| `salary_source` | VARCHAR(32) | 是 | salary_source，例如 API |
| `salary_min_monthly_yuan` | INT UNSIGNED | 是 | 月薪下限，单位元 |
| `salary_max_monthly_yuan` | INT UNSIGNED | 是 | 月薪上限，单位元 |
| `salary_months` | TINYINT UNSIGNED | 是 | 13薪、14薪等；未知为 NULL |
| `location_name` | VARCHAR(255) | 是 | location 原文 |
| `city_name` | VARCHAR(100) | 是 | 地点第一段 |
| `area_name` | VARCHAR(100) | 是 | 地点第二段；空段转 NULL |
| `business_district_name` | VARCHAR(100) | 是 | 地点第三段；空段转 NULL |
| `experience_text` | VARCHAR(100) | 是 | tags 第一项，需规则确认 |
| `education_text` | VARCHAR(100) | 是 | tags 第二项，需规则确认 |
| `recruiter_name` | VARCHAR(255) | 是 | 当前输出没有稳定来源 |
| `recruiter_title` | VARCHAR(255) | 是 | boss_title |
| `recruiter_active_text` | VARCHAR(100) | 是 | BOSS 最近一次被 Collector 观察到在线的带时区 ISO-8601 时间文本；不参与 contentHash |
| `remote_type` | VARCHAR(32) | 否 | UNKNOWN、ONSITE、HYBRID、REMOTE |
| `job_status` | VARCHAR(32) | 否 | UNKNOWN、ACTIVE、OFFLINE |
| `detail_status` | VARCHAR(32) | 否 | UNKNOWN、FETCHED、FAILED、UNAVAILABLE |
| `detail_collected_at` | DATETIME(3) | 是 | 详情实际采集时间，UTC |
| `source_tags` | JSON | 是 | tags 拆分后的来源标签 |
| `source_skill_tags` | JSON | 是 | skills 拆分后的来源标签，不等同 AI 标准技能 |
| `welfare` | JSON | 是 | welfare 拆分后的福利数组 |
| `created_at` | DATETIME(3) | 否 | 创建时间，UTC |
| `updated_at` | DATETIME(3) | 否 | 更新时间，UTC |

约束：

1. `information_id` 同时作为主键和外键。
2. `information_id` 关联 `information_item.id`。
3. 删除当前主信息时，职位扩展可以级联删除。
4. `salary_text` 必须保留来源原文。
5. 薪资无法可靠解析时，三个标准化薪资字段保持 `NULL`。
6. `remote_type` 默认 `UNKNOWN`。
7. 搜索结果中存在的职位可以标记为 `ACTIVE`。
8. 一次搜索未出现某职位，不得直接标记为 `OFFLINE`。
9. 不得将 `boss_name` 映射为 `recruiter_name`。
10. `details.skill_tags` 不映射到 `source_skill_tags`。

推荐索引：

```text
INDEX (source_company_id)
INDEX (source_recruiter_id)
INDEX (company_name)
INDEX (city_name)
INDEX (job_status, updated_at)
INDEX (remote_type, updated_at)
INDEX (salary_min_monthly_yuan, salary_max_monthly_yuan)
INDEX (detail_status)
```

## 10. 字段解析规则

### 10.1 tags

当前样本：

```text
5-10年 | 本科
经验不限 | 大专
```

处理：

```text
source_tags = ["5-10年", "本科"]
experience_text = "5-10年"
education_text = "本科"
```

Mapper 必须先验证标签是否匹配经验和学历规则，不能永久依赖固定位置。

无法通过规则识别经验或学历时，对应标准字段保持 `NULL`，仅在 `source_tags` 中保留来源标签，不基于固定位置猜测。

`job_labels` 在当前 90 条样本中与 `tags` 完全重复，因此不单独建立数据库字段，只保留在 `raw_payload.list` 中。

### 10.2 skills

当前 `skills` 不仅包含技术技能，还可能包含：

- 不接受居家办公
- 团队管理经验
- 211/985
- 入职社保
- 面试定薪定级

因此标准字段命名为：

```text
source_skill_tags
```

未来 AI 产生的标准技能应存入分析结果，不得覆盖来源标签。

### 10.3 welfare

以 `|` 分割：

1. 去除首尾空白。
2. 删除空值。
3. 保持首次出现顺序。
4. 删除完全重复项。
5. 原始字符串仍保留在 rawPayload。

### 10.4 location

当前样本都是三段形式：

```text
成都·武侯区·中和
成都··
```

解析：

```text
成都··
city_name = 成都
area_name = NULL
business_district_name = NULL
```

始终保留完整 `location_name`。解析失败时只保存原文。

### 10.5 salary

当前 90 条样本均符合月薪格式：

```text
20-40K
15-30K·14薪
```

示例：

```text
15-30K·14薪
salary_min_monthly_yuan = 15000
salary_max_monthly_yuan = 30000
salary_months = 14
```

解析失败时：

- 保留 `salary_text`
- 保留 `salary_source`
- 标准薪资字段设为 `NULL`

### 10.6 详情状态

匹配到详情且 `jd` 非空：

```text
detail_status = FETCHED
```

当前旧详情文件没有详情采集时间：

```text
detail_collected_at = NULL
```

列表中没有对应详情时，仅凭这两份文件无法判断是未抓取还是失败：

```text
detail_status = UNKNOWN
```

未来 Collector 应输出明确状态。

Phase 1 的 `detail_status` 仅支持：

```text
UNKNOWN
FETCHED
FAILED
UNAVAILABLE
```

## 11. information_snapshot

用途：保存首次接入版本，以及当前业务内容发生变化后的不可变历史版本。

| 字段 | 类型 | 允许为空 | 说明 |
|---|---|---:|---|
| `id` | BIGINT UNSIGNED | 否 | 主键，自增 |
| `information_id` | BIGINT UNSIGNED | 否 | 关联 information_item.id |
| `version_no` | INT UNSIGNED | 否 | 该信息的版本号，从 1 开始 |
| `content_hash` | CHAR(64) | 否 | 该版本业务内容 Hash |
| `title` | VARCHAR(1000) | 否 | 该版本标题 |
| `content` | LONGTEXT | 是 | 该版本正文 |
| `standardized_payload` | JSON | 否 | 该版本通用字段和 Job 扩展字段 |
| `raw_payload` | JSON | 否 | 该版本安全清理后的原始业务对象 |
| `collected_at` | DATETIME(3) | 否 | 该版本采集时间 |
| `collector_id` | VARCHAR(128) | 否 | Collector 实例 |
| `collector_version` | VARCHAR(64) | 否 | Collector 版本 |
| `created_at` | DATETIME(3) | 否 | 快照创建时间 |

约束与索引：

```text
UNIQUE (information_id, version_no)
INDEX (information_id, content_hash)
INDEX (information_id, created_at)
```

不再使用：

```text
UNIQUE (information_id, content_hash)
```

原因是职位可能经历：

```text
版本 A → 版本 B → 恢复为版本 A
```

恢复为 A 仍是一次新的时间变化，必须能够保存新版本。

写入规则：

1. 首次创建时：
   - `current_version_no = 1`
   - 插入 `version_no = 1` 的快照。
2. Hash 未变化：
   - 不增加版本号。
   - 不插入快照。
3. Hash 变化：
   - 在事务和行锁保护下将 `current_version_no + 1`。
   - 插入对应版本号快照。
4. 快照不可更新。
5. `information_snapshot.information_id` 外键采用 `ON DELETE RESTRICT`，防止误删历史。

## 12. rawPayload 结构

建议每个职位的中央 `raw_payload` 使用：

```json
{
  "list": {
    "job_id": "sanitized-local-join-id",
    "title": "Java后端开发工程师",
    "salary": "20-35K",
    "salary_source": "api",
    "location": "成都·武侯区·中和",
    "tags": "5-10年 | 本科",
    "boss_name": "示例公司",
    "boss_title": "HR",
    "encrypt_job_id": "sanitized-source-job-id",
    "encrypt_boss_id": "sanitized-source-recruiter-id",
    "encrypt_brand_id": "sanitized-source-company-id",
    "job_link": "https://www.zhipin.com/job_detail/example.html"
  },
  "detail": {
    "job_id": "sanitized-local-join-id",
    "jd": "脱敏后的职位描述",
    "skill_tags": []
  }
}
```

中央 rawPayload 默认移除：

- `security_id`
- `lid`
- Cookie
- Authorization
- 浏览器本地凭证

本地原始文件是否保留这些字段，由 Collector 本地安全策略决定，但不得提交到 Git。

## 13. collectionContext

列表根级信息映射到：

```json
{
  "runId": "generated-or-source-run-id",
  "keyword": "java",
  "city": "成都",
  "queryFilters": {},
  "filterDescriptions": [],
  "resultTotal": 90,
  "sourceScrapedAt": "2026-07-22T23:14:09.656461+08:00",
  "timeZoneAssumption": "Asia/Shanghai"
}
```

`collection_context`：

- 不参与幂等键。
- 不参与 contentHash。
- 不得包含 Cookie、Token 或浏览器指纹。

## 14. Phase 1 Flyway 规划

第一版迁移：

```text
V1__create_information_job_and_snapshot_tables.sql
```

创建：

- `information_item`
- `job_information`
- `information_snapshot`

后续示例：

```text
V2__create_analysis_tables.sql
V3__create_user_and_recommendation_tables.sql
V4__create_notification_tables.sql
```

## 15. 已冻结设计决策

1. `information_snapshot.information_id` 外键采用 `ON DELETE RESTRICT`。
2. `security_id` 和 `lid` 在提交 Information Hub 前删除。
3. 历史无时区时间按 `Asia/Shanghai` 解释，并在 `collection_context` 中记录该假设。
4. 新采集时间必须包含明确时区。
5. `detail_status` 仅支持 `UNKNOWN`、`FETCHED`、`FAILED`、`UNAVAILABLE`。
6. `tags` 使用规则识别经验和学历；无法识别时只保留 `source_tags`。
7. BOSS 的 `source_item_id` 使用 `encrypt_job_id`。
8. `job_id` 只用于 Collector 内部合并列表和详情，不作为平台标识或标准数据库字段。
9. `boss_name` 映射 `company_name`。
10. `encrypt_boss_id` 映射 `source_recruiter_id`。
11. 快照使用递增 `version_no`，支持 `A → B → A`。
12. 重复接入采用非破坏性更新，缺失、`NULL`、空字符串或空数组不覆盖已有有效值。
13. BOSS `bossOnline=true` 记录带时区的 Collector 在线观测时间；false 或缺失时不清空已有观测时间。
14. `recruiter_active_text` 不参与 contentHash，仅该字段变化时不创建职位快照。
