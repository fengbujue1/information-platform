# Information Platform · BOSS 直聘采集器 v2.1.0

本目录是 Information Platform 当前使用的 BOSS 直聘职位采集器。它通过 Chrome DevTools Protocol（CDP）连接一个独立、真实的 Chrome，复用用户本人在该浏览器中的登录态，从 BOSS 搜索接口获取职位列表和明文薪资，并按需抓取职位详情。

采集结果会先保存为本地 JSON/CSV；启用 Information Hub 配置后，再映射为统一 `InformationEnvelope` 并逐条提交给后端。Hub 暂时不可用不会让已完成的本地采集失败，可重试数据会进入本地 Outbox，之后独立补传。

当前版本：`2.1.0`。要求 Python 3.10+、Google Chrome，以及用户本人可正常使用的 BOSS 直聘账号。

> 本工具仅用于个人求职、学习和技术研究。请遵守 BOSS 直聘用户协议、访问规则及所在地法律法规，控制采集范围和频率，不要用于商业转售、批量滥用、绕过安全校验或对目标网站造成负担。

## 1. 实际工作流程

```text
启动 BOSS 专用 Chrome
→ 用户手动登录 BOSS 直聘
→ Collector 通过 CDP 检查登录状态
→ 搜索 API 获取职位列表与明文薪资
→ 可选：逐个打开详情页获取 JD
→ JSON/CSV 本地落盘
→ 可选：映射为 InformationEnvelope 并提交 Information Hub
→ Hub 可重试失败时写入 Outbox
→ Hub 恢复后独立补传
```

重要事实：

- 不使用 Selenium 或 Playwright；采集器直接连接真实 Chrome 的 CDP。
- 默认使用独立、持久化的 Chrome Profile，不读取或覆盖主 Chrome Profile。
- 列表优先使用 BOSS 搜索 API；DOM 降级默认关闭，因为 DOM 薪资可能不可靠。
- 详情抓取默认开启，只把明确的职位描述区域当作 JD。
- 本地文件始终先保存；Information Hub 提交默认关闭且是 best-effort。
- Collector 不直接访问平台数据库。

## 2. 目录与输出

所有默认路径都相对于本采集器目录，而不是终端当前目录：

```text
boss-zhipin-scraper/
├── config/
│   ├── collector.ini.example   # 无秘密的配置模板
│   └── collector.ini           # 本地真实配置，Git 已忽略
├── data/city_codes.json        # 本地城市码表
├── integrations/               # Hub 映射、HTTP Client、Outbox
├── result/
│   ├── chrome-profile/         # BOSS 专用 Chrome 登录态
│   ├── job-result/             # 列表、详情、CSV、分析结果
│   │   └── raw-responses/      # 显式开启的原始响应诊断文件
│   └── outbox/
│       ├── pending/            # 等待补传
│       ├── quarantine/         # 损坏文件
│       └── rejected/           # 永久失败文件
├── scripts/boss_cdp_raw.py     # 主入口
└── scripts/job_summary.py      # 离线摘要与提示词
```

`result/`、真实 `collector.ini`、虚拟环境和浏览器 Profile 均已被 Git 忽略，不要手工提交这些内容。

## 3. 首次安装

以下命令默认在仓库根目录执行。

### Windows PowerShell（推荐）

```powershell
Set-Location collectors/boss-zhipin-scraper

py -3.10 -m venv .venv
Set-ExecutionPolicy -Scope Process Bypass
& .\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
```

如果本机 `py -3.10` 不存在，可以使用任意 Python 3.10+ 的实际命令创建 `.venv`。

### macOS / Linux

```bash
cd collectors/boss-zhipin-scraper
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
```

项目也支持 [uv](https://docs.astral.sh/uv/)：

```bash
uv sync
```

运行入口时可使用激活后的 `python`，也可使用 `uv run python`。

## 4. 第一次登录与环境检查

### 4.1 启动专用 Chrome

```powershell
python scripts/boss_cdp_raw.py --setup-chrome
```

该命令会：

1. 创建或复用 `result/chrome-profile/`；
2. 在默认端口 `9222` 启动真实 Chrome；
3. 打开 BOSS 直聘页面；
4. 等待你在这个专用浏览器中手动登录；
5. 用搜索接口确认登录态可用并能返回明文薪资。

登录态保存在专用 Profile 中，正常情况下只需首次手动登录。重复运行 `--setup-chrome` 不会清空登录态，也不会影响主 Chrome。

不要日常使用 `--copy-login-state`。只有明确需要从主 Chrome 导入 Cookie 时才执行：

```powershell
python scripts/boss_cdp_raw.py --setup-chrome --copy-login-state
```

若需要清空专用浏览器登录态并重建：

```powershell
python scripts/boss_cdp_raw.py --setup-chrome --reset-chrome-profile
```

### 4.2 检查环境

保持专用 Chrome 运行，然后执行：

```powershell
python scripts/boss_cdp_raw.py --check
```

检查项包括 Python 依赖、CDP 端口和 BOSS 登录状态。还可以执行一次不写结果文件的真实接口检查：

```powershell
python scripts/boss_cdp_raw.py --smoke-test
```

只有 `--check` 通过后再开始正式采集。

## 5. 采集职位

### 5.1 最小命令

```powershell
python scripts/boss_cdp_raw.py --keyword "Java 后端" --city 成都 --pages 1
```

这会采集列表和详情，并默认保存到：

```text
result/job-result/boss_jobs_YYYYMMDD_HHMM.json
result/job-result/boss_details_YYYYMMDD_HHMM.json
```

### 5.2 常用命令

```powershell
# 三页职位，详情默认开启
python scripts/boss_cdp_raw.py --keyword "Java 后端" --city 成都 --pages 3
python scripts/boss_cdp_raw.py --keyword "Java 远程 居家" --city 全国 --pages 5

# 仅列表，不打开职位详情
python scripts/boss_cdp_raw.py --keyword "Java 后端" --city 成都 --pages 3 --no-detail

# 最多抓 10 个详情
python scripts/boss_cdp_raw.py --keyword "Java 后端" --city 成都 --pages 3 --max-details 10

# 同时生成 CSV
python scripts/boss_cdp_raw.py --keyword "Java 后端" --city 成都 --pages 3 --format csv

# 抓取后在终端输出聚合分析
python scripts/boss_cdp_raw.py --keyword "Java 后端" --city 成都 --pages 3 --analysis

# 正常完成后关闭 BOSS 专用 Chrome
python scripts/boss_cdp_raw.py --keyword "Java 后端" --city 成都 --pages 1 --close-chrome
```

单次页数最大为 10，超过时会自动调整为 10。建议从 1 页开始验证，不要高频、无节制重复执行。

### 5.3 城市与筛选条件

```powershell
# 查看所有支持城市
python scripts/boss_cdp_raw.py --list-cities

# 按名称过滤城市
python scripts/boss_cdp_raw.py --list-cities 江

# 组合筛选
python scripts/boss_cdp_raw.py `
  --keyword "Java 后端" `
  --city 成都 `
  --pages 2 `
  --scale 304 `
  --salary 405 `
  --experience 104 `
  --degree 203
```

常用代码：

| 参数 | 示例 | 含义 |
| --- | --- | --- |
| `--scale` | `304` | 公司规模，304 为 500～999 人 |
| `--stage` | `807` | 融资阶段，807 为已上市 |
| `--salary` | `405` | 薪资范围，405 为 10～20K |
| `--experience` | `104` | 经验要求，104 为 1～3 年 |
| `--degree` | `203` | 学历要求，203 为本科 |
| `--industry` | `1001` | 行业，1001 为互联网 |

运行 `python scripts/boss_cdp_raw.py --help` 可查看完整代码说明。

### 5.4 自定义输出与合并

```powershell
# 指定列表和详情文件
python scripts/boss_cdp_raw.py `
  --keyword "Java 后端" `
  --city 成都 `
  --pages 1 `
  --output result/job-result/java-jobs.json `
  --detail-output result/job-result/java-details.json

# 把本轮职位与已有列表按 job_id 去重合并
python scripts/boss_cdp_raw.py `
  --keyword "Java 后端" `
  --city 成都 `
  --pages 1 `
  --merge result/job-result/old-jobs.json `
  --output result/job-result/merged-jobs.json
```

## 6. 写入 Information Hub

仅需要本地 JSON/CSV 时可以跳过本节。Hub 提交默认关闭，不会产生任何后端请求。

### 6.1 前置条件

先确认 Information Hub 已启动，并且服务端配置了 Collector Token。服务端完整配置参见 [`../../backend/information-hub/CONFIGURATION.md`](../../backend/information-hub/CONFIGURATION.md)。

Collector 与后端必须使用同一个安全随机 Token。PowerShell 可这样生成：

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

不要把真实 Token 提交到 Git、粘贴到截图或写入日志。

### 6.2 创建 Collector 配置

```powershell
Copy-Item config/collector.ini.example config/collector.ini
```

编辑 `config/collector.ini`：

```ini
[collector]
collector_id = boss-collector-desktop
collector_version = 2.1.0
historical_timezone = Asia/Shanghai

[information_hub]
enabled = true
url = http://127.0.0.1:8080/api/v1/collector/items
collector_token = 替换为与后端一致的真实Token
connect_timeout_seconds = 3
read_timeout_seconds = 10
```

如果后端在其他主机或端口，请填写 Collector 所在机器实际可访问的完整写入接口 URL。

配置优先级：

```text
环境变量 > INI 文件 > 程序默认值
```

可用环境变量：

- `INFORMATION_HUB_ENABLED`
- `INFORMATION_HUB_URL`
- `INFORMATION_HUB_COLLECTOR_TOKEN`
- `INFORMATION_HUB_CONNECT_TIMEOUT_SECONDS`
- `INFORMATION_HUB_READ_TIMEOUT_SECONDS`

### 6.3 采集并自动提交

默认配置文件是 `config/collector.ini`，启用后原采集命令无需增加参数：

```powershell
python scripts/boss_cdp_raw.py --keyword "Java 后端" --city 成都 --pages 1
```

也可以使用仓库外的配置：

```powershell
python scripts/boss_cdp_raw.py `
  --config C:\secure\collector.ini `
  --keyword "Java 后端" `
  --city 成都 `
  --pages 1
```

实际顺序：

```text
列表和详情本地保存成功
→ 安全映射 InformationEnvelope
→ 单条 POST 到 Information Hub
```

提交规则：

- 2xx：成功；
- 4xx / 413：当前数据或配置的永久问题，记录失败后继续，不进入 Outbox；
- 5xx、超时、连接失败或非预期响应：停止本批后续 HTTP 请求，把当前及剩余安全 Envelope 写入 Outbox；
- Hub 配置、映射或网络错误不会改变已完成的本地采集结果。

### 6.4 Hub 恢复后补传

```powershell
python scripts/boss_cdp_raw.py --flush-outbox
```

指定其他配置：

```powershell
python scripts/boss_cdp_raw.py --flush-outbox --config C:\secure\collector.ini
```

补传命令不会启动 Chrome，也不会访问 BOSS。成功文件从 `pending/` 删除；损坏文件移入 `quarantine/`；4xx/413 等永久失败移入 `rejected/`。可重试失败使用从 60 秒到最多 1 小时的指数退避。

同一职位重复补传是安全的，Information Hub 使用来源与 `sourceItemId` 做幂等写入。

详细字段映射、Outbox 语义和 Python 调用方式参见 [`INTEGRATION.md`](./INTEGRATION.md)。

## 7. 离线摘要与提示词

`job_summary.py` 只读取已保存的职位列表和详情，不访问 Chrome、BOSS 或 Information Hub，也不读取本地简历。

```powershell
# 自动读取 result/job-result 下最新结果
python scripts/job_summary.py

# 指定输入
python scripts/job_summary.py `
  --input result/job-result/java-jobs.json `
  --details result/job-result/java-details.json `
  --top 15

# 只输出摘要或提示词
python scripts/job_summary.py --summary-only
python scripts/job_summary.py --prompt-only
```

安装为包后也可以使用：

```powershell
boss-scraper --help
boss-summary --top 15
```

## 8. 原始响应诊断

仅在核对 BOSS 来源字段或排查接口响应时开启：

```powershell
python scripts/boss_cdp_raw.py `
  --keyword "Java 后端" `
  --city 成都 `
  --pages 1 `
  --no-detail `
  --capture-raw-response
```

原始响应保存到 `result/job-result/raw-responses/`。它可能包含 `security_id`、`lid` 等来源追踪字段：

- 不要提交 Git；
- 不要直接发送到 Information Hub；
- 不要对外分享；
- 排查完成后按需清理。

脚本不会为此功能采集请求头、响应头、Cookie、Authorization 或完整 Chrome Profile。

## 9. 参数速查

| 参数 | 用途 |
| --- | --- |
| `--keyword` | 搜索关键词，默认 `AI Agent` |
| `--city` | 中文城市名或城市代码，默认上海 |
| `--pages` | 页数，默认 3，最大 10 |
| `--detail` / `--no-detail` | 开启或关闭详情 JD；默认开启 |
| `--max-details` | 限制详情数量 |
| `--format json\|csv` | 输出格式；CSV 模式仍保留 JSON |
| `--output` | 列表 JSON 输出路径 |
| `--detail-output` | 详情 JSON 输出路径 |
| `--analysis` | 在主命令结束时输出分析报告 |
| `--input` | 读取已有列表 JSON，跳过列表采集 |
| `--merge` | 按 `job_id` 合并已有列表 |
| `--check` | 检查依赖、CDP 和登录状态 |
| `--smoke-test` | 真实搜索接口检查，不写结果 |
| `--setup-chrome` | 启动/复用专用 Chrome |
| `--stop-chrome` | 只关闭专用 Profile 对应的 Chrome |
| `--close-chrome` | 本次采集正常结束后关闭专用 Chrome |
| `--cdp-port` | CDP 端口，默认 9222 |
| `--list-cities [关键词]` | 查看本地城市码表 |
| `--allow-dom-fallback` | API 无数据时允许 DOM 降级；默认关闭 |
| `--capture-raw-response` | 保存原始搜索响应，仅用于本地诊断 |
| `--config` | 指定 Collector INI 配置 |
| `--flush-outbox` | 独立补传 Information Hub Outbox |

完整参数以实时帮助为准：

```powershell
python scripts/boss_cdp_raw.py --help
python scripts/job_summary.py --help
```

## 10. 常见问题

### 无法连接 `127.0.0.1:9222`

```powershell
python scripts/boss_cdp_raw.py --setup-chrome
python scripts/boss_cdp_raw.py --check
```

如果端口被其他 CDP Chrome 占用，关闭旧实例，或为 setup、check 和采集统一指定其他端口，例如 `--cdp-port 9223`。

### 显示未登录

打开采集器启动的专用 Chrome，在其中完成 BOSS 登录或安全验证，然后重新执行 `--check`。不要因为“未登录”提示而反复重建 Profile。

### 显示环境异常、访问频繁或需要安全验证

采集器会停止，不会自动绕过风控。请在专用 Chrome 中完成平台要求的验证，降低执行频率并稍后重试。不要连续运行登录探测或强制 DOM 降级。

### 搜索没有数据

先用 `--smoke-test` 和常见关键词/城市验证登录态，再检查筛选条件。默认不使用 DOM fallback；只有明确接受薪资可能不可靠时才使用 `--allow-dom-fallback`。

### Hub 没有收到数据

依次检查：

1. `config/collector.ini` 的 `enabled = true`；
2. URL 是完整的 `/api/v1/collector/items`；
3. Collector Token 与后端完全一致；
4. 后端已启动且 Collector 所在机器可以访问；
5. `result/outbox/pending/` 是否有待补传文件；
6. 修复服务后运行 `--flush-outbox`。

4xx/413 不会自动进入 Outbox，应根据后端错误码修正 Token、URL、payload 或请求体上限。

### 如何安全关闭 Chrome

```powershell
python scripts/boss_cdp_raw.py --stop-chrome
```

该命令按采集器独立 Profile 精确匹配进程，不会关闭主 Chrome。

### 能否从任意工作目录启动

可以。脚本会按自身路径定位采集器根目录。Windows 示例：

```powershell
& 'D:\0.project\information-platform\collectors\boss-zhipin-scraper\.venv\Scripts\python.exe' `
  'D:\0.project\information-platform\collectors\boss-zhipin-scraper\scripts\boss_cdp_raw.py' `
  --keyword "Java 后端" `
  --city 成都 `
  --pages 1
```

## 11. 测试

本模块使用标准库 `unittest`，大多数测试通过 mock 隔离真实 Chrome 和网络：

```powershell
Set-Location collectors/boss-zhipin-scraper
python -m unittest discover -s tests -p "test_*.py"
```

修改采集器前请先阅读 [`AGENTS.md`](./AGENTS.md)。Chrome/CDP、登录、请求、翻页、去重和本地输出属于保护区域；Information Hub 接入通过 `integrations/` 保持解耦。

## 12. 安全边界

- 只使用 BOSS 专用 Chrome Profile；不要把 CDP 端口暴露到公网。
- `collector.ini`、Token、Cookie、Profile 和采集结果不得提交 Git。
- 发往 Hub 的 `rawPayload` 会递归移除 `security_id`、`lid` 和凭证类字段。
- Information Hub 只接收统一协议；Collector 不连接主数据库。
- 不把登录墙、截断详情、公司介绍或推荐职位误当成完整 JD。
- 不自动绕过验证码、登录限制或平台风控。

## 13. 相关文档

- [`INTEGRATION.md`](./INTEGRATION.md)：字段映射、Hub Client 和 Outbox 详细语义
- [`AGENTS.md`](./AGENTS.md)：采集器开发边界
- [`CHANGELOG.md`](./CHANGELOG.md)：版本变更
- [`../../backend/information-hub/CONFIGURATION.md`](../../backend/information-hub/CONFIGURATION.md)：Information Hub 服务端配置
- [`README.en.md`](./README.en.md)：上游英文说明；部分路径和集成信息可能未同步，以本中文 README 和实际代码为准

## License

MIT
