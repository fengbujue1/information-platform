# Information Platform 数据库设计

设计版本：1.2  
当前阶段：Phase 1  
数据库：MySQL 8.x  
字符集：utf8mb4  
时间存储：UTC

## 1. 设计目标

Phase 1 支持 BOSS 职位接入，但公共数据库模型不能绑定 BOSS。

Phase 1 创建：

- `information_item`
- `job_information`
- `information_snapshot`

后续通过 Flyway 新增 AI、推荐、用户和通知相关表。

## 2. 核心原则

1. 通用字段进入 `information_item`。
2. 职位专有字段进入 `job_information`。
3. 历史版本进入 `information_snapshot`。
4. 原始业务 payload 完整保留。
5. 不为所有来源字段建立数据库列。
6. 经常筛选、排序和统计的字段才标准化。
7. 来源发布时间未知时保持 `NULL`。
8. first/last seen 由服务端维护。
9. 所有迁移由 Flyway 管理。
10. 已在共享环境执行的迁移不得修改，只能新增迁移。
11. 数据库不使用 ENUM，业务枚举由 Java 校验。

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

`content_hash` 使用 SHA-256，输入为合并完成后的规范化业务内容：

- title
- content
- sourceUrl
- 标准化 Job extension

不包含：

- Collector 实例
- Collector 版本
- 采集时间
- first/last seen
- collectionContext
- rawPayload 中仅用于追踪的易变字段

## 4. 非破坏性合并

重复提交同一幂等键时：

1. 有效新值覆盖旧值。
2. 缺失、`null` 和空字符串不覆盖已有有效值。
3. 空数组默认不覆盖已有非空数组。
4. 最近一次 rawPayload、采集时间和 Collector 元数据正常更新。
5. 合并完成后计算 Hash。
6. Hash 未变化时，不插入快照。
7. Hash 变化时，更新当前表并插入新快照。

V1 不支持通过普通接入请求主动清空字段。

## 5. 时间规则

- API 接收 ISO-8601 带偏移时间。
- Java 使用 `OffsetDateTime`。
- 写入数据库前统一转换为 UTC。
- MySQL 使用 `DATETIME(3)` 保存 UTC。
- API 输出 UTC `Z` 或显式偏移。
- `publish_time` 不能用 `collected_at` 填充。

## 6. information_item

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
| `collected_at` | DATETIME(3) | 否 | 最近一次采集时间，UTC |
| `first_seen_time` | DATETIME(3) | 否 | 服务端首次发现时间，UTC |
| `last_seen_time` | DATETIME(3) | 否 | 服务端最近发现时间，UTC |
| `content_hash` | CHAR(64) | 否 | 最终业务内容 SHA-256 |
| `raw_payload` | JSON | 否 | 最近一次原始业务对象 |
| `schema_version` | INT UNSIGNED | 否 | 接入协议版本 |
| `collector_id` | VARCHAR(128) | 否 | 最近一次采集器实例 |
| `collector_version` | VARCHAR(64) | 否 | 最近一次采集器版本 |
| `collection_context` | JSON | 是 | 最近一次脱敏采集上下文 |
| `status` | VARCHAR(32) | 否 | ACTIVE、ARCHIVED、DELETED、UNKNOWN |
| `created_at` | DATETIME(3) | 否 | 创建时间，UTC |
| `updated_at` | DATETIME(3) | 否 | 更新时间，UTC |

约束与索引：

- 唯一索引：`source, information_type, source_item_id`
- 索引：`information_type, last_seen_time`
- 索引：`source, last_seen_time`
- 索引：`status, last_seen_time`
- 索引：`publish_time`

`information_item.status` 表示平台内部生命周期，不表示 BOSS 职位是否失效。

## 7. job_information

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
| `salary_months` | TINYINT UNSIGNED | 是 | 例如 13 薪 |
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
- 不得将 `boss_name` 映射为 `recruiter_name`。

`job_status` 表示来源职位业务状态。

一次搜索未出现该职位，不得直接标记为 `OFFLINE`。

## 8. information_snapshot

用途：保存首次接入版本和业务内容发生变化后的历史版本。

| 字段 | 类型 | 允许为空 | 说明 |
|---|---|---:|---|
| `id` | BIGINT UNSIGNED | 否 | 主键，自增 |
| `information_id` | BIGINT UNSIGNED | 否 | 关联 information_item.id |
| `content_hash` | CHAR(64) | 否 | 该版本业务内容 Hash |
| `title` | VARCHAR(1000) | 否 | 该版本标题 |
| `content` | LONGTEXT | 是 | 该版本正文 |
| `standardized_payload` | JSON | 否 | 该版本标准化业务字段 |
| `raw_payload` | JSON | 否 | 该版本原始业务对象 |
| `collected_at` | DATETIME(3) | 否 | 该版本采集时间 |
| `collector_id` | VARCHAR(128) | 否 | 采集器实例 |
| `collector_version` | VARCHAR(64) | 否 | 采集器版本 |
| `created_at` | DATETIME(3) | 否 | 快照创建时间 |

约束与索引：

- 外键：`information_id` 关联 `information_item.id`
- 唯一索引：`information_id, content_hash`
- 索引：`information_id, created_at`

写入规则：

1. 首次创建信息时插入一个快照。
2. 相同 Hash 重复接入时不插入快照。
3. Hash 变化时插入新快照。
4. 快照是不可变记录，Phase 1 不提供更新操作。
5. 删除主信息时是否级联删除快照，由 Flyway 实施前再次确认；推荐使用 `ON DELETE RESTRICT`，避免误删历史。

## 9. 当前 BOSS 字段事实

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

- `boss_name` 来自 `brandName`，应映射为公司名称。
- `tags` 当前把经验和学历拼接在一起。
- `skills`、`job_labels`、`welfare` 当前是 ` | ` 分隔字符串。
- 招聘者姓名与活跃状态当前没有稳定结构化输出。

## 10. Flyway 规划

Phase 1：

```text
V1__create_information_job_and_snapshot_tables.sql
```

后续：

```text
V2__create_analysis_tables.sql
V3__create_user_and_recommendation_tables.sql
```
