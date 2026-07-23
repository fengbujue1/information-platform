# Information Platform 数据库设计

设计版本：1.0  
当前阶段：Phase 1  
数据库：MySQL 8.x  
字符集：utf8mb4

## 1. 设计目标

Information Platform 是一个通用的信息采集、归档、分析和推荐平台。

当前第一种信息类型是 BOSS 职位信息，但数据库公共模型不能绑定 BOSS，也不能只为招聘场景服务。

未来可能接入：

- `JOB`：招聘职位
- `NEWS`：新闻资讯
- `GOVERNMENT`：政府政策
- `HOUSE_PRICE`：房价信息
- `EDUCATION`：教育信息
- `REPORT`：行业报告
- `OTHER`：其他信息

当前 Phase 1 只实现：

- `information_item`
- `job_information`

当前不创建：

- `information_snapshot`
- `analysis_task`
- `analysis_result`
- `user_profile`
- `recommendation_record`
- `notification_task`

这些表将在后续 Phase 通过新的 Flyway 迁移增加。

## 2. 核心设计原则

1. `information_item` 保存所有信息类型的公共字段。
2. `job_information` 保存职位专有字段。
3. Job 只是 Information 的一种类型。
4. 不建立包含所有业务字段的超级大表。
5. 采集器原始 JSON 必须完整保存。
6. 不为每一个 BOSS 原始字段创建数据库列。
7. 经常需要查询、筛选、排序的字段才标准化为数据库列。
8. 来源发布时间未知时，`publish_time` 必须为 `NULL`。
9. 不得使用采集时间代替来源发布时间。
10. `first_seen_time` 和 `last_seen_time` 由服务端维护。
11. 所有数据库结构通过 Flyway 管理。
12. 不使用 MyBatis、Hibernate 自动建表。
13. 已执行到共享环境的 Flyway 文件不得直接修改。
14. 数据库字段使用 `snake_case`。
15. Java 字段使用 `camelCase`。

## 3. 信息幂等规则

统一幂等键：

```text
source + information_type + source_item_id
```

第一次接收到该数据时：

1. 插入 `information_item`。
2. 插入对应的 `job_information`。
3. 设置 `first_seen_time`。
4. 设置 `last_seen_time`。

再次接收到相同幂等键时：

1. 不插入新的主信息记录。
2. 更新 `last_seen_time`。
3. 更新允许变化的当前字段。
4. 比较 `content_hash`。
5. 内容未变化时，不重复创建后续 AI 任务。
6. 内容发生变化时，后续可保存快照并重新分析。

## 4. information_item

| 字段 | 类型 | 允许为空 | 说明 |
|---|---|---:|---|
| `id` | BIGINT UNSIGNED | 否 | 主键，自增 |
| `information_type` | VARCHAR(32) | 否 | JOB、NEWS 等 |
| `source` | VARCHAR(64) | 否 | BOSS、RSS 等 |
| `source_item_id` | VARCHAR(255) | 否 | 来源平台中的稳定唯一标识 |
| `source_url` | VARCHAR(2048) | 是 | 来源页面地址 |
| `title` | VARCHAR(1000) | 否 | 标题 |
| `content` | LONGTEXT | 是 | 信息主要正文 |
| `publish_time` | DATETIME(3) | 是 | 来源真实发布时间 |
| `collected_at` | DATETIME(3) | 否 | 本次提交中的采集时间 |
| `first_seen_time` | DATETIME(3) | 否 | 系统第一次发现时间 |
| `last_seen_time` | DATETIME(3) | 否 | 系统最近一次发现时间 |
| `content_hash` | CHAR(64) | 是 | 标准化内容 SHA-256 |
| `raw_payload` | JSON | 否 | 原始采集数据 |
| `schema_version` | INT UNSIGNED | 否 | 接入协议版本 |
| `status` | VARCHAR(32) | 否 | ACTIVE、OFFLINE 等 |
| `created_at` | DATETIME(3) | 否 | 创建时间 |
| `updated_at` | DATETIME(3) | 否 | 更新时间 |

唯一约束：

```text
source + information_type + source_item_id
```

推荐索引：

- `information_type + last_seen_time`
- `source + last_seen_time`
- `publish_time`
- `status + last_seen_time`

## 5. job_information

| 字段 | 类型 | 允许为空 | 说明 |
|---|---|---:|---|
| `information_id` | BIGINT UNSIGNED | 否 | 主键，关联 information_item.id |
| `source_company_id` | VARCHAR(255) | 是 | 来源平台公司 ID |
| `company_name` | VARCHAR(255) | 是 | 公司名称 |
| `salary_text` | VARCHAR(100) | 是 | 来源原始薪资文本 |
| `salary_min_monthly_yuan` | INT UNSIGNED | 是 | 标准化月薪下限，单位元 |
| `salary_max_monthly_yuan` | INT UNSIGNED | 是 | 标准化月薪上限，单位元 |
| `location_name` | VARCHAR(255) | 是 | 原始工作地点文本 |
| `city_name` | VARCHAR(100) | 是 | 城市 |
| `area_name` | VARCHAR(100) | 是 | 区县 |
| `experience_text` | VARCHAR(100) | 是 | 经验要求 |
| `education_text` | VARCHAR(100) | 是 | 学历要求 |
| `recruiter_name` | VARCHAR(255) | 是 | 招聘者姓名 |
| `recruiter_title` | VARCHAR(255) | 是 | 招聘者职位 |
| `recruiter_active_text` | VARCHAR(100) | 是 | 招聘者活跃状态原文 |
| `remote_type` | VARCHAR(32) | 否 | UNKNOWN、ONSITE、HYBRID、REMOTE |
| `job_status` | VARCHAR(32) | 否 | UNKNOWN、ACTIVE、OFFLINE |
| `skills` | JSON | 是 | 技能列表 |
| `welfare` | JSON | 是 | 福利列表 |
| `created_at` | DATETIME(3) | 否 | 创建时间 |
| `updated_at` | DATETIME(3) | 否 | 更新时间 |

约束：

- `information_id` 是主键。
- `information_id` 外键关联 `information_item.id`。
- 删除主信息时，可以级联删除职位扩展。
- `salary_text` 必须保留来源原文。
- 薪资无法可靠解析时，标准化薪资字段保持 `NULL`。
- `remote_type` 第一阶段默认 `UNKNOWN`。

## 6. 时间字段说明

- `publish_time`：来源网站明确提供的真实发布时间，未知时为 `NULL`。
- `collected_at`：采集器本次获取数据的时间。
- `first_seen_time`：Information Hub 第一次收到该信息的时间。
- `last_seen_time`：Information Hub 最近一次收到该信息的时间。
- `created_at` / `updated_at`：数据库记录创建和更新时间。

## 7. Phase 1 Flyway 规划

```text
V1__create_information_and_job_tables.sql
```

只创建：

- `information_item`
- `job_information`

后续迁移示例：

```text
V2__create_information_snapshot.sql
V3__create_analysis_tables.sql
V4__create_user_and_recommendation_tables.sql
```

## 8. 待确认事项

以下内容需要根据 BOSS 采集器实际输出确认：

- BOSS 稳定职位 ID 对应哪个原始字段
- 公司 ID 字段名称
- 招聘者活跃状态字段名称
- skills 和 welfare 的实际数据结构
- 城市、区域、地址字段的实际来源
- 是否存在可信的来源发布时间
- 职位失效状态的实际字段
