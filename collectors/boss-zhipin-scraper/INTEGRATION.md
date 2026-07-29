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
├── information_mapper.py
└── hub_client.py
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

映射器只接收已经加载的列表根对象和详情根对象/数组，返回可 JSON 序列化的 InformationEnvelope V1 字典列表。它不读取真实 Chrome Profile，也不实现网络请求或 Outbox。

核心行为：

- 使用 `job_id` 在 Collector 内部左连接，缺少详情仍输出列表职位。
- 使用 `encrypt_job_id` 构建 `sourceItemId`。
- 列表字段为标准化主来源，详情补充 JD；重复字段冲突只记录字段名和 job_id，不记录字段值。
- 同一批次共享一个 `runId`；历史无时区 `scraped_at` 按配置时区解释。
- `boss_online_observed_at` 只在 `boss_online` 严格为 true 且时间带时区时映射。
- rawPayload 递归移除 Hub 禁止的敏感字段，但保留安全的列表与详情业务字段。

## 4. Information Hub Client

TASK-007 已实现默认关闭的独立 HTTP Client。Collector 只在列表和详情完成本地保存后，才按单条 InformationEnvelope 调用 Hub。

推荐复制配置模板并填写本机或服务器配置：

```powershell
Copy-Item config/collector.ini.example config/collector.ini
```

```ini
[collector]
collector_id = boss-collector-desktop
collector_version = 2.1.0
historical_timezone = Asia/Shanghai

[information_hub]
enabled = true
url = http://127.0.0.1:8080/api/v1/collector/items
collector_token = replace-with-real-token
connect_timeout_seconds = 3
read_timeout_seconds = 10
```

默认读取项目内的 `config/collector.ini`，也可以通过命令行指定其他位置：

```powershell
python scripts/boss_cdp_raw.py --config C:\secure\collector.ini --keyword "AI Agent" --city 上海 --pages 3
```

脚本入口使用 `Path(__file__).resolve()` 定位 Collector 项目根目录，并在导入同级 `integrations` 包前初始化模块搜索路径。因此，也可以从任意工作目录使用解释器和脚本的绝对路径启动，不需要执行 `Set-Location` 或设置 `PYTHONPATH`：

```powershell
& 'D:\0.project\information-platform\collectors\boss-zhipin-scraper\.venv\Scripts\python.exe' `
  'D:\0.project\information-platform\collectors\boss-zhipin-scraper\scripts\boss_cdp_raw.py' `
  --config 'D:\0.project\information-platform\collectors\boss-zhipin-scraper\config\collector.ini' `
  --keyword "AI Agent" `
  --city 上海 `
  --pages 1
```

默认配置、结果和 Chrome Profile 路径仍然相对于 Collector 项目定位，不会随终端当前目录变化。

环境变量仍然可用，并且优先级高于 INI，适合 CI 或临时覆盖：

```text
INFORMATION_HUB_ENABLED
INFORMATION_HUB_URL
INFORMATION_HUB_COLLECTOR_TOKEN
INFORMATION_HUB_CONNECT_TIMEOUT_SECONDS
INFORMATION_HUB_READ_TIMEOUT_SECONDS
```

完整优先级为：环境变量 > INI 配置 > 程序默认值。真实 `collector.ini` 已被 Git 忽略，只提交无密码的 `.example` 模板。

规则：

- `INFORMATION_HUB_ENABLED` 默认是 `false`，关闭时不执行 Mapper，也不发起 HTTP 请求。
- `INFORMATION_HUB_URL` 是单条写入接口的完整 URL。
- Token 只放入 `Authorization: Bearer ...` 请求头，不写入日志。
- 2xx 视为成功；4xx 和 413 记录单条失败后继续下一条。
- 5xx、非预期状态、超时或连接失败时停止本批次剩余请求，避免累积等待。
- 配置、映射或网络失败不会改变原采集命令的退出结果。
- 5xx、非预期状态、超时、连接失败、请求失败和未知客户端异常属于可重试失败；当前项及本批尚未发送的 Envelope 原子写入本地 Outbox。
- 4xx、413、配置错误和映射错误不进入 Outbox，避免无效请求无限重试。

### 本地 Outbox 与补传

TASK-008 使用 Collector 本地文件系统保存待补传的、已经安全清理的 InformationEnvelope：

```text
result/outbox/
├── pending/       # 等待补传或等待退避时间到期
├── quarantine/    # JSON 损坏或结构不合法，待人工检查
└── rejected/      # 补传时收到 4xx/413 等永久失败响应
```

Outbox 文件不包含 Token、请求头、Cookie 或 Collector 配置。新文件通过同目录临时文件、`fsync` 和原子替换写入；初次立即可补传，后续失败采用 60 秒起步、最大 1 小时的指数退避。

Information Hub 恢复并且 Collector 配置有效后，执行独立补传命令：

```powershell
python scripts/boss_cdp_raw.py --flush-outbox

# 使用非默认配置文件
python scripts/boss_cdp_raw.py --flush-outbox --config C:\secure\collector.ini
```

该命令不会初始化 Chrome，也不会访问 BOSS。它只处理已经到期的 `pending` 文件；成功后删除，损坏文件移入 `quarantine`，补传时确认无法重试的响应移入 `rejected`。某次补传再次遇到可重试失败时，会更新重试次数和下次时间并停止本轮，避免故障期间连续请求。

补传可以从任意工作目录通过脚本绝对路径执行，不需要 `Set-Location`。同一业务键可能被重复补传，服务端依靠 `source + sourceItemId` 的幂等写入保证安全。

也可以直接从 Python 调用：

```python
from integrations import submit_boss_results_to_hub

result = submit_boss_results_to_hub(list_root, detail_root_or_array)
```

Information Hub 服务端的数据库、Token 和外部 YAML 配置参见 [Information Hub 配置说明](../../backend/information-hub/CONFIGURATION.md)。

## 5. 执行顺序

```text
采集列表
→ 保存列表文件
→ 采集详情
→ 保存详情文件
→ 按 job_id 合并
→ 构建 InformationEnvelope
→ 提交 Hub
→ 可重试失败时将当前及剩余安全 Envelope 原子写入 Outbox
→ 采集流程正常结束
→ 后续通过 --flush-outbox 独立补传
```

## 6. 保护区域

不得为接入 Hub 而重写：

- Chrome 启动；
- CDP 连接；
- 登录检测；
- BOSS 请求；
- 翻页；
- 现有本地 JSON/CSV 输出。

## 7. 当前时间输出与未来建议

当前新采集列表的 `scraped_at` 使用 UTC `Z` 时间；历史无时区文件仍按配置的 `Asia/Shanghai` 解释。

TASK-006B 已实现为列表职位增加：

```json
{
  "boss_online": true,
  "boss_online_observed_at": "2026-07-27T05:11:12.345Z"
}
```

`boss_online_observed_at` 只在 `boss_online=true` 时生成，表示最近一次被 Collector 观察到在线的时间，不是 BOSS 官方最后活跃时间。

未来建议列表和详情都增加稳定批次信息：

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

## 8. 安全

发送 Hub 前删除：

- security_id
- lid
- Cookie
- Token
- 浏览器凭证

本地采集结果不得提交 Git。

## 9. token生成方式
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $bytes = New-Object byte[] 32
    $generator.GetBytes($bytes)
    $token = [Convert]::ToBase64String($bytes)
    $generator.Dispose()
    $token
