# Information Platform 数据库设计

设计版本：1.1  
当前阶段：Phase 1  
数据库：MySQL 8.x  
字符集：utf8mb4  
时间存储：UTC

## 1. 设计目标

Phase 1 支持 BOSS 职位接入，但公共数据库模型不能绑定 BOSS。

当前只创建：

- `information_item`
- `job_information`

后续再通过 Flyway 新增快照、AI、推荐、用户和通知相关表。

## 2. 核心原则

1. 通用字段进入 `information_item`。
2. 职位专有字段进入 `job_information`。
3. 原始业务 payload 完整保留。
4. 不为所有来源字段建立数据库列。
5. 经常筛选、排序和统计的字段才标准化。
6. 来源发布时间未知时保持 `NULL`。
7. first/last seen 由服务端维护。
8. 所有迁移由 Flyway 管理。
9. 已在共享环境执行的迁移不得修改，只能新增迁移。
10. 数据库不使用 ENUM，业务枚举由 Java 校验。

## 3. 幂等与变化判断

幂等键：

```text
source + information_type + source_item_id
```

BOSS 当前预计映射：

```text
source_item_id = encrypt_job_id
```

最终以 TASK-001 审计结果为准。

`content_hash` 使用 SHA-256，输入为规范化业务内容：

- title
- content
- sourceUrl
- 标准化 Job extension

不包含采集器实例、采集时间、first/last seen 等易变元数据。

## 4. 时间规则

- API 接收 ISO-8601 带偏移时间。
- Java 使用 `OffsetDateTime`。
- 写入数据库前统一转换为 UTC。
- MySQL 使用 `DATETIME(3)` 保存 UTC。
- API 输出 UTC `Z` 或显式偏移。
- `publish_time` 不能用 `collected_at` 填充。

## 5. information_item

| 字段 | 类型 | 允许为空 | 说明 |
|---|---|---:|---|
| `id` | BIGINT UNSIGNED | 否 | 主键，自增 |
| `information_type` | VARCHAR(32) | 否 | JOB、NEWS 等 |
| `source` | VARCHAR(64) | 否 | BOSS、RSS 等 |
| `source_item_id` | VARCHAR(255) | 否 | 来源稳定唯一 ID |
| `source_url` | VARCHAR(2048) | 是 | 来源页面地址 |
| `title` | VARCHAR(1000) | 否 | 标题 |
| `content` | LONGTEXT | 是 | 主要正文 |
| `publish_time` | DATETIME(3) | 是 | 来源真实发布时间，UTC |
| `collected_at` | DATETIME(3) | 否 | 采集器获取时间，UTC |
| `first_seen_time` | DATETIME(3) | 否 | 服务端首次发现时间，UTC |
| `last_seen_time` | DATETIME(3) | 否 | 服务端最近发现时间，UTC |
| `content_hash` | CHAR(64) | 否 | 规范化业务内容 SHA-256 |
| `raw_payload` | JSON | 否 | 原始业务对象 |
| `schema_version` | INT UNSIGNED | 否 | 接入协议版本 |
| `collector_id` | VARCHAR(128) | 否 | 采集器实例标识 |
| `collector_version` | VARCHAR(64) | 否 | 采集器版本 |
| `status` | VARCHAR(32) | 否 | ACTIVE、ARCHIVED、UNKNOWN |
| `created_at` | DATETIME(3) | 否 | 创建时间，UTC |
| `updated_at` | DATETIME(3) | 否 | 更新时间，UTC |

约束与索引：

- 唯一索引：`source, information_type, source_item_id`
- 索引：`information_type, last_seen_time`
- 索引：`source, last_seen_time`
- 索引：`status, last_seen_time`
- 索引：`publish_time`

## 6. job_information

| 字段 | 类型 | 允许为空 | 说明 |
|---|---|---:|---|
| `information_id` | BIGINT UNSIGNED | 否 | 主键和外键 |
| `source_company_id` | VARCHAR(255) | 是 | BOSS `encrypt_brand_id` |
| `company_name` | VARCHAR(255) | 是 | 当前列表中的 `boss_name` 实际为品牌名 |
| `company_url` | VARCHAR(2048) | 是 | 公司来源页面 |
| `company_scale_text` | VARCHAR(100) | 是 | 公司规模原文 |
| `company_stage_text` | VARCHAR(100) | 是 | 融资阶段原文 |
| `company_industry_text` | VARCHAR(255) | 是 | 行业原文 |
| `salary_text` | VARCHAR(100) | 是 | 原始薪资文本 |
| `salary_min_monthly_yuan` | INT UNSIGNED | 是 | 标准化月薪下限 |
| `salary_max_monthly_yuan` | INT UNSIGNED | 是 | 标准化月薪上限 |
| `salary_months` | TINYINT UNSIGNED | 是 | 例如 13 薪；未知为空 |
| `location_name` | VARCHAR(255) | 是 | 原始地点文本 |
| `city_name` | VARCHAR(100) | 是 | 城市 |
| `area_name` | VARCHAR(100) | 是 | 区县 |
| `business_district_name` | VARCHAR(100) | 是 | 商圈 |
| `experience_text` | VARCHAR(100) | 是 | 经验要求 |
| `education_text` | VARCHAR(100) | 是 | 学历要求 |
| `recruiter_name` | VARCHAR(255) | 是 | 当前 Collector 尚无稳定来源 |
| `recruiter_title` | VARCHAR(255) | 是 | 当前可映射 `boss_title` |
| `recruiter_active_text` | VARCHAR(100) | 是 | 当前 Collector 尚无稳定来源 |
| `remote_type` | VARCHAR(32) | 否 | UNKNOWN、ONSITE、HYBRID、REMOTE |
| `job_status` | VARCHAR(32) | 否 | UNKNOWN、ACTIVE、OFFLINE |
| `job_labels` | JSON | 是 | 职位标签数组 |
| `skills` | JSON | 是 | 技能数组 |
| `welfare` | JSON | 是 | 福利数组 |
| `created_at` | DATETIME(3) | 否 | 创建时间 |
| `updated_at` | DATETIME(3) | 否 | 更新时间 |

约束：

- `information_id` 外键关联 `information_item.id`。
- 删除主信息时级联删除职位扩展。
- 薪资无法可靠解析时，标准薪资字段保持 `NULL`。
- `remote_type` 默认 `UNKNOWN`。
- 不得将 `boss_name` 直接映射为 `recruiter_name`。

## 7. 当前 BOSS 字段事实

当前列表对象主要字段：

- `title`
- `salary`
- `salary_source`
- `location`
- `tags`
- `boss_name`
- `boss_title`
- `company_scale`
- `company_stage`
- `company_industry`
- `job_labels`
- `skills`
- `security_id`
- `lid`
- `encrypt_job_id`
- `encrypt_boss_id`
- `encrypt_brand_id`
- `job_link`
- `company_link`
- `welfare`

注意：

- `boss_name` 来自 BOSS API 的 `brandName`，应映射为公司名称。
- `tags` 当前把经验和学历拼接在一起。
- `skills`、`job_labels`、`welfare` 当前是 ` | ` 分隔字符串。
- 详情提取提供经过校验的 JD 和详情标签。
- 招聘者姓名与活跃状态当前没有稳定结构化输出。

## 8. Flyway 规划

Phase 1：

```text
V1__create_information_and_job_tables.sql
```

后续：

```text
V2__create_information_snapshot.sql
V3__create_analysis_tables.sql
V4__create_user_and_recommendation_tables.sql
```
