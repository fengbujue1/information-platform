# Information Platform 集成说明

## 1. 当前输出

列表文件结构：

```text
keyword
city
filters
filter_desc
scraped_at
total
jobs[]
```

职位详情文件当前是裸数组：

```text
[
  {
    "job_id": "...",
    "jd": "..."
  }
]
```

## 2. 当前样本结论

- 列表 90 条。
- 详情 25 条。
- 详情通过 job_id 关联列表。
- 平台 sourceItemId 使用 encrypt_job_id。
- boss_name 是公司名。
- encrypt_boss_id 是来源招聘者 ID。
- boss_title 是招聘者职位。
- job_labels 与 tags 当前重复。
- details.skill_tags 当前不是可靠技能。
- 详情文件没有 run_id、scraped_at 和状态。
- 列表 scraped_at 没有时区。

## 3. Information Platform 适配层

TASK-006 已实现纯映射适配层：

```text
integrations/
├── __init__.py
├── config.py
├── boss_job_merger.py
└── information_mapper.py
```

使用示例：

```python
from integrations import MapperConfig, map_boss_results

config = MapperConfig(
    collector_id="boss-collector-desktop",
    collector_version="2.1.0",
    historical_timezone="Asia/Shanghai",
)
envelopes = map_boss_results(list_root, detail_root_or_array, config)
```

映射器只接收已经加载的列表根对象和详情根对象/数组，返回可 JSON 序列化的 InformationEnvelope V1 字典列表。它不读取真实 Chrome Profile，不发送 HTTP，也不实现 Outbox。

核心行为：

- 使用 `job_id` 在 Collector 内部左连接，缺少详情仍输出列表职位。
- 使用 `encrypt_job_id` 构建 `sourceItemId`。
- 列表字段为标准化主来源，详情补充 JD；重复字段冲突只记录字段名和 job_id，不记录字段值。
- 同一批次共享一个 `runId`；历史无时区 `scraped_at` 按配置时区解释。
- `boss_online_observed_at` 只在 `boss_online` 严格为 true 且时间带时区时映射。
- rawPayload 递归移除 Hub 禁止的敏感字段，但保留安全的列表与详情业务字段。

## 4. 执行顺序

```text
采集列表
→ 保存列表文件
→ 采集详情
→ 保存详情文件
→ 按 job_id 合并
→ 构建 InformationEnvelope
→ 提交 Hub
→ 失败写 Outbox
```

## 5. 保护区域

不得为接入 Hub 而重写：

- Chrome 启动；
- CDP 连接；
- 登录检测；
- BOSS 请求；
- 翻页；
- 现有本地 JSON/CSV 输出。

## 6. 未来输出建议

列表和详情都增加：

```json
{
  "run_id": "uuid",
  "scraped_at": "2026-07-22T23:14:09.656461+08:00"
}
```

新采集时间必须包含明确时区偏移或 `Z`；历史无时区时间按 `Asia/Shanghai` 解释。

每个详情增加：

```json
{
  "detail_status": "FETCHED",
  "detail_collected_at": "2026-07-22T23:14:10.000000+08:00",
  "error_code": null
}
```

TASK-006B 完成后，列表职位增加：

```json
{
  "boss_online": true,
  "boss_online_observed_at": "2026-07-27T05:11:12.345Z"
}
```

`boss_online_observed_at` 只在 `boss_online=true` 时生成，表示最近一次被 Collector 观察到在线的时间，不是 BOSS 官方最后活跃时间。

## 7. 安全

发送 Hub 前删除：

- security_id
- lid
- Cookie
- Token
- 浏览器凭证

本地采集结果不得提交 Git。
