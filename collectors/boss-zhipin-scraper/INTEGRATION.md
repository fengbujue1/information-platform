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

推荐：

```text
integrations/
├── config.py
├── boss_file_loader.py
├── boss_job_merger.py
├── information_mapper.py
├── hub_client.py
└── outbox.py
```

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
