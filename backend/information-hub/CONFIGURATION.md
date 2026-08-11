# Information Hub 配置说明

## 1. 配置范围

本文件说明 Information Hub Spring Boot 服务端的数据库、Flyway、Session、Collector、Identity、AI Processing 和 Recommendation 运行配置。

Collector 如何连接 Information Hub，参见 [BOSS Collector 集成说明](../../collectors/boss-zhipin-scraper/INTEGRATION.md)。

## 2. 创建本地配置文件

Spring Boot 原生支持从外部 `config/application.yml` 加载配置。先在仓库根目录复制示例模板：

```powershell
Copy-Item backend/information-hub/config/application.yml.example backend/information-hub/config/application.yml
```

然后修改 `backend/information-hub/config/application.yml` 中的数据库连接、数据库密码和 Collector Token，并按实际运行范围配置 Bootstrap、Preview、AI Provider、Batch/Schedule 与 Recommendation Worker。

真实 `application.yml` 已被 Git 忽略，仓库只提交不包含真实秘密的 `application.yml.example`。

## 3. 本地启动

### 3.1 建立远程 MySQL 的 SSH 隧道

当前共享开发数据库运行在远程服务器的 Docker MySQL 容器中。MySQL 宿主机端口只绑定远程服务器的 `127.0.0.1:3306`，不直接向公网开放，因此本地运行 Information Hub 时必须先建立 SSH 隧道。

已按部署说明配置本机 OpenSSH 别名后，在单独的 PowerShell 窗口中运行：

```powershell
ssh -N -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes -L 13306:127.0.0.1:3306 information-platform-server
```

该命令运行期间不会返回正常命令提示符。Information Hub 使用数据库期间必须保持此窗口和 SSH 连接开启，停止时在该窗口按 `Ctrl+C`。

在另一个 PowerShell 窗口中验证本地隧道：

```powershell
Test-NetConnection 127.0.0.1 -Port 13306
```

只有看到以下结果，才表示本地端口已经可以通过隧道访问远程 MySQL：

```text
TcpTestSucceeded : True
```

默认配置中的数据库地址应与该本地转发端口一致：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:13306/information_hub_dev
```

如果因为端口占用而修改了 SSH 隧道的本地端口，必须同步修改 `spring.datasource.url`。SSH 密钥、本机别名、服务器部署和数据库连接验证的完整步骤参见 [远程 MySQL 部署与运维说明](../../deploy/README.md#三建立-ssh-隧道)。

只有改用本机 MySQL 或其他已通过安全网络直接可达的数据库，并相应修改数据源地址时，才不依赖上述 SSH 隧道。当前项目约定的共享开发数据库需要使用该隧道。

### 3.2 启动 Information Hub

从 Information Hub 模块目录启动：

```powershell
Set-Location backend/information-hub
.\mvnw.cmd spring-boot:run
```

Spring Boot 会读取当前模块下的 `config/application.yml`。

也可以在 IntelliJ IDEA 中直接运行 `InformationHubApplication.main()`，但运行配置必须满足：

- Project SDK/JRE 使用 Java 21。
- Working directory 设置为 `backend/information-hub` 的绝对路径。
- SSH 隧道已经建立并保持运行。

如果 Working directory 是仓库根目录，Spring Boot 不会自动找到嵌套在 `backend/information-hub/config/` 下的外部配置文件。此时应优先修正 Working directory；也可以显式增加以下启动参数：

```text
--spring.config.additional-location=optional:file:./backend/information-hub/config/
```

当 `spring.flyway.enabled=false` 时，数据库连接可能延迟到首次执行数据库操作才创建。因此，控制台显示应用启动成功不代表数据库一定可以访问；仍应先通过上述端口检查，并实际验证需要访问数据库的接口。当 `spring.flyway.enabled=true` 时，应用会在启动阶段执行数据库迁移，SSH 隧道未建立或数据库配置错误通常会直接导致启动失败。

## 4. 服务器部署

使用可执行 JAR 部署时，将 `config/application.yml` 放在 JAR 当前工作目录下：

```text
information-hub/
├── information-hub.jar
└── config/
    └── application.yml
```

在 `information-hub` 目录中启动 JAR，Spring Boot 即可读取外部配置：

```powershell
java -jar information-hub.jar
```

## 5. 配置项总表

下表只列出本项目在 `src/main/resources/application.yml` 中显式定义的配置。Spring Boot
还支持大量框架通用配置，但未在项目中声明的通用选项不属于本表范围。

“来源”表示这个值应由谁决定或从哪里取得，不表示要把值提交进仓库。

### 5.1 Spring、数据库与 Session

| YAML 配置键 | 环境变量 | 含义、格式与默认值 | 值的来源 |
| --- | --- | --- | --- |
| `spring.application.name` | 无 | 应用内部名称，固定为 `information-hub` | 项目代码固定 |
| `spring.datasource.url` | `INFORMATION_HUB_DB_URL` | MySQL JDBC URL；示例 `jdbc:mysql://127.0.0.1:13306/information_hub_dev?...` | 数据库管理员、Docker/部署配置；共享开发库的主机端口来自 SSH 隧道方案 |
| `spring.datasource.username` | `INFORMATION_HUB_DB_USERNAME` | MySQL 登录用户名；示例 `information_hub_dev` | 数据库管理员创建并授权 |
| `spring.datasource.password` | `INFORMATION_HUB_DB_PASSWORD` | MySQL 登录密码；敏感，无默认值 | 数据库管理员或部署 Secret，与数据库用户名配套 |
| `spring.flyway.enabled` | `INFORMATION_HUB_FLYWAY_ENABLED` | 是否启动时执行 Migration；`true/false`，默认 `false` | 由部署/迁移流程决定；测试或正式迁移时显式开启 |
| `spring.flyway.locations` | 无 | Migration classpath 目录，固定 `classpath:db/migration` | 项目目录结构固定 |
| `spring.flyway.validate-on-migrate` | 无 | 迁移前校验历史脚本，固定 `true` | 项目数据库安全策略固定 |
| `spring.flyway.clean-disabled` | 无 | 禁止 Flyway clean 删除数据库对象，固定 `true` | 项目数据保护策略固定 |
| `server.servlet.session.cookie.http-only` | 无 | 禁止 JavaScript 读取 Session Cookie，固定 `true` | 项目 Web 安全策略固定 |
| `server.servlet.session.cookie.same-site` | 无 | SameSite 策略，项目固定 `lax` | 项目同源 Session/CSRF 设计固定 |
| `server.servlet.session.cookie.secure` | `INFORMATION_HUB_SESSION_COOKIE_SECURE` | 是否仅通过 HTTPS 传 Cookie；默认 `false`，生产 HTTPS 必须 `true` | 根据实际部署入口是 HTTP 还是 HTTPS 决定 |
| `server.servlet.session.cookie.path` | 无 | Cookie 生效路径，固定 `/` | 同源 Web/API 路径设计固定 |
| `mybatis-plus.configuration.map-underscore-to-camel-case` | 无 | 将数据库 `snake_case` 列映射到 Java `camelCase` 字段，固定 `true` | 项目持久化命名约定固定 |

### 5.2 Collector 与初始账号

| YAML 配置键 | 环境变量 | 含义、格式与默认值 | 值的来源 |
| --- | --- | --- | --- |
| `information-hub.collector-api.token` | `INFORMATION_HUB_COLLECTOR_TOKEN` | Collector 写入接口 Bearer Token；敏感，无默认值 | 使用安全随机数自行生成；后端与 Collector 配置同一值，不从第三方获取 |
| `information-hub.collector-api.max-request-bytes` | `INFORMATION_HUB_COLLECTOR_MAX_REQUEST_BYTES` | 单次请求体字节上限；正整数，默认 `2097152`（2 MiB） | 项目默认；只有实际 payload 规模变化时评估调整 |
| `information-hub.identity.bootstrap.username` | `INFORMATION_HUB_BOOTSTRAP_USERNAME` | 空账号库首次启动时创建的登录名；为空不执行 Bootstrap | 平台管理员自行确定 |
| `information-hub.identity.bootstrap.password` | `INFORMATION_HUB_BOOTSTRAP_PASSWORD` | 初始明文密码；敏感，只在启动内存中编码为摘要 | 平台管理员使用密码管理器或安全随机方式生成 |
| `information-hub.identity.bootstrap.display-name` | `INFORMATION_HUB_BOOTSTRAP_DISPLAY_NAME` | 页面显示名，可为空；示例 `平台管理员` | 平台管理员自行确定 |
| `information-hub.identity.bootstrap.timezone` | `INFORMATION_HUB_BOOTSTRAP_TIMEZONE` | IANA Zone ID，默认 `Asia/Shanghai` | 从 Java/IANA tz database 支持的时区名称中选择 |

Bootstrap 只在 `user_account` 为空时创建一次账号。库中已有任意账号时，修改这些变量不会修改
现有用户名或密码。

### 5.3 Batch 与 Schedule

| YAML 配置键 | 环境变量 | 含义、格式与默认值 | 值的来源 |
| --- | --- | --- | --- |
| `information-hub.ai.batch.worker-enabled` | `INFORMATION_HUB_AI_BATCH_WORKER_ENABLED` | 是否允许 Worker 执行 Batch Item；默认 `false` | 部署管理员在 Provider 配置完整、确认允许产生调用费用后开启 |
| `information-hub.ai.batch.worker-initial-delay` | `INFORMATION_HUB_AI_BATCH_WORKER_INITIAL_DELAY` | 启动后首次轮询延迟；Spring Duration，默认 `3s` | 项目默认；联调可用 `200ms`，生产按启动负载调整 |
| `information-hub.ai.batch.worker-fixed-delay` | `INFORMATION_HUB_AI_BATCH_WORKER_FIXED_DELAY` | 上轮完成到下轮开始的间隔；Spring Duration，默认 `1s` | 项目默认；根据数据库和 Provider 吞吐调整 |
| `information-hub.ai.schedule.dispatcher-enabled` | `INFORMATION_HUB_AI_SCHEDULE_DISPATCHER_ENABLED` | 全局 Dispatcher 开关；默认 `true`，每条新 Schedule 仍默认关闭 | 部署管理员决定是否运行调度扫描 |
| `information-hub.ai.schedule.dispatcher-initial-delay` | `INFORMATION_HUB_AI_SCHEDULE_DISPATCHER_INITIAL_DELAY` | 启动后首次扫描延迟；Spring Duration，默认 `5s` | 项目默认；按启动负载调整 |
| `information-hub.ai.schedule.dispatcher-fixed-delay` | `INFORMATION_HUB_AI_SCHEDULE_DISPATCHER_FIXED_DELAY` | 两轮扫描间隔；Spring Duration，默认 `30s` | 项目默认；按允许的触发延迟和数据库负载调整 |
| `information-hub.ai.schedule.misfire-grace` | `INFORMATION_HUB_AI_SCHEDULE_MISFIRE_GRACE` | 延迟后仍可按原计划点触发的宽限窗口；默认 `5m` | 冻结 Schedule Contract 默认；按业务可接受延迟评估 |
| `information-hub.ai.schedule.max-schedules-per-poll` | `INFORMATION_HUB_AI_SCHEDULE_MAX_PER_POLL` | 单轮最多处理的到期 Schedule 数；默认 `100` | 项目默认；根据 Schedule 数量和数据库容量调整 |

Spring Duration 可写成 `200ms`、`3s`、`5m`、`1h`。值必须为正数；不要填写没有单位的模糊时间字符串。

### 5.4 Preview 与 AI Provider

| YAML 配置键 | 环境变量 | 含义、格式与默认值 | 值的来源 |
| --- | --- | --- | --- |
| `information-hub.ai.preview.hmac-secret` | `INFORMATION_HUB_PREVIEW_HMAC_SECRET` | Preview Token 的 HmacSHA256 秘密；敏感，至少 32 个 UTF-8 字节 | 使用安全随机数自行生成；不是 Provider API Key，也不从 AI 平台获取 |
| `information-hub.ai.provider.enabled` | `INFORMATION_HUB_AI_ENABLED` | 是否允许调用真实 Provider；默认 `false` | 部署管理员在其余 Provider 项完整后显式开启 |
| `information-hub.ai.provider.base-url` | `INFORMATION_HUB_AI_BASE_URL` | OpenAI-compatible API 基础地址；通常包含 `/v1`，后端追加 `/chat/completions` | 所选 Provider 的官方 OpenAI-compatible API 文档；不要使用网页控制台地址 |
| `information-hub.ai.provider.api-key` | `INFORMATION_HUB_AI_API_KEY` | Provider API Key；敏感，无默认值 | Provider 官方控制台的 API Key/凭据页面创建，只放服务端环境变量或 Secret Manager |
| `information-hub.ai.provider.model` | `INFORMATION_HUB_AI_MODEL` | Chat Completions 使用的模型 ID | Provider 官方模型列表；填写 API 模型 ID，不是营销展示名称 |
| `information-hub.ai.provider.timeout` | `INFORMATION_HUB_AI_TIMEOUT` | HTTP 连接和读取超时；Spring Duration，必须大于 0，默认 `2m` | 项目默认；按 Provider 延迟和部署网络调整 |
| `information-hub.ai.provider.max-output-tokens` | `INFORMATION_HUB_AI_MAX_OUTPUT_TOKENS` | 单次最大输出 Token；整数 `1..5000`，默认 `5000` | Phase 3 Definition/Contract 冻结上限，不应按模型最大上下文随意扩大 |

Base URL 示例形状：

```text
https://provider.example.com/v1
```

后端会请求：

```text
https://provider.example.com/v1/chat/completions
```

因此不要把 `/chat/completions` 再写入 `INFORMATION_HUB_AI_BASE_URL`。Provider 的真实域名、模型 ID
和 Key 必须以该 Provider 的官方文档/控制台为准；仓库不会替你分配这些值。

### 5.5 Recommendation Worker

| YAML 配置键 | 环境变量 | 含义、格式与默认值 | 值的来源 |
| --- | --- | --- | --- |
| `information-hub.recommendation.worker-enabled` | `INFORMATION_HUB_RECOMMENDATION_WORKER_ENABLED` | 是否消费既有 `PENDING` Recommendation Run；默认 `false` | 需要执行手动刷新或 Batch 自动触发产生的 Run 时由部署管理员显式开启 |
| `information-hub.recommendation.worker-initial-delay` | `INFORMATION_HUB_RECOMMENDATION_WORKER_INITIAL_DELAY` | 启动后首次扫描延迟；Spring Duration，默认 `3s` | 项目默认；联调可按启动时序调整 |
| `information-hub.recommendation.worker-fixed-delay` | `INFORMATION_HUB_RECOMMENDATION_WORKER_FIXED_DELAY` | 上轮完成到下一轮开始的间隔；Spring Duration，默认 `1s` | 项目默认；按数据库和本地计算吞吐调整 |
| `information-hub.recommendation.worker-stale-after` | `INFORMATION_HUB_RECOMMENDATION_WORKER_STALE_AFTER` | `RUNNING` Run 超过该阈值后恢复为 `PENDING`；Spring Duration，默认 `5m` | 项目确定性重试边界；只在有实际超时依据时调整 |

Manual Refresh 只创建 `PENDING` Run。前端会持续轮询，直到 Run 进入 `COMPLETED`、`FAILED` 或 `NOOP`；如果 Recommendation Worker 保持默认关闭，Run 不会被消费。开启 Recommendation Worker 不会调用 AI Provider，它只消费数据库中已有的成功 Analysis，并执行确定性的 Candidate、Scoring 和 Ranking。

在 PowerShell 中使用环境变量时，必须在启动后端的同一个进程环境中设置并重新启动：

```powershell
$env:INFORMATION_HUB_RECOMMENDATION_WORKER_ENABLED = "true"
Set-Location backend/information-hub
.\mvnw.cmd spring-boot:run
```

使用 IntelliJ IDEA 时，应将变量配置在实际启动 `InformationHubApplication` 的 Run Configuration 中；在另一个 PowerShell 窗口临时设置变量不会影响已打开的 IDE 或已运行的 Java 进程。

## 6. 环境变量覆盖与生效范围

Spring Boot 使用 `${环境变量:默认值}` 读取上述变量。环境变量适合 CI、部署 Secret 和本地临时覆盖；
外部 `config/application.yml` 适合本地受控配置。若两处同时配置，应以启动时 Spring Environment
实际解析结果为准，排查时优先确认启动命令、工作目录和外部配置路径。

环境变量只在**进程启动时继承**。后端启动后再执行 `SetEnvironmentVariable`，正在运行的 Java
进程不会自动获得新值；必须停止后端，并从能够查到该变量的新 PowerShell/IDE Run Configuration
重新启动。

检查敏感变量时只检查“是否设置”和长度，不要打印原值。例如：

```powershell
$secret = $env:INFORMATION_HUB_PREVIEW_HMAC_SECRET
[pscustomobject]@{
    IsSet = -not [string]::IsNullOrWhiteSpace($secret)
    Utf8Bytes = if ($null -eq $secret) { 0 } else {
        [Text.Encoding]::UTF8.GetByteCount($secret)
    }
}
```

Preview HMAC Secret 的 `IsSet` 必须为 `True`，`Utf8Bytes` 必须大于或等于 32。

### 6.1 Phase 3 全栈 E2E

TASK-032 的自动化 E2E 会启动真实 Information Hub、真实 Vite Web、专用 MySQL
测试库和本机 OpenAI-compatible Fake Provider。运行前设置以下仅供测试编排器读取的变量：

```text
INFORMATION_HUB_E2E_DB_URL
INFORMATION_HUB_E2E_DB_USERNAME
INFORMATION_HUB_E2E_DB_PASSWORD
INFORMATION_HUB_E2E_USERNAME
INFORMATION_HUB_E2E_PASSWORD
INFORMATION_HUB_E2E_COLLECTOR_TOKEN
INFORMATION_HUB_E2E_PREVIEW_HMAC_SECRET
```

其中数据库名称必须包含 `test` 或 `e2e`，且不得包含 `dev`。Preview HMAC Secret
必须至少包含 32 个 UTF-8 字节。测试使用合成 JOB 数据，不把真实职位隐私写入 fixture。

```powershell
Set-Location frontend/information-hub-web
npm.cmd run test:e2e:phase3
```

该命令的 Fake Provider 只监听本机随机端口，使用运行时随机 API Key，并检查后端输出和
浏览器响应中是否出现 API Key 或 rawPayload 标记。CI 不读取也不需要真实 Provider Key。

真实 Provider 验收仍使用本节前述 `INFORMATION_HUB_AI_*` 服务端变量，并同时显式启用
Batch Worker；只允许选择少量 JOB Snapshot。验收结束后必须关闭临时 Schedule，并移除
Provider API Key 环境变量。

### 6.2 Phase 4 全栈 E2E

TASK-044 的全栈 E2E 使用真实 Information Hub、真实 V4 测试 MySQL、本机 Fake Provider、Vite 与 Chromium，并由运行器显式开启 Analysis Batch Worker 和 Recommendation Worker。运行前设置：

```text
INFORMATION_HUB_E2E_DB_URL
INFORMATION_HUB_E2E_DB_USERNAME
INFORMATION_HUB_E2E_DB_PASSWORD
```

也可以复用对应的 `INFORMATION_HUB_TEST_DB_URL/USERNAME/PASSWORD`。数据库名称必须包含 `test` 或 `e2e` 且不得包含 `dev`。测试账号、Collector Token、Preview Secret 和 Fake Provider Key 由运行器在内存中生成，不需要真实 Provider Key。

```powershell
Set-Location backend/information-hub
.\mvnw.cmd -DskipTests package
Set-Location ../../frontend/information-hub-web
npm.cmd run test:e2e:phase4
```

## 7. 安全要求

- 不要把真实数据库密码、Token、Cookie 或服务器秘密写入 `.example` 文件。
- 服务器上的 `application.yml` 应只允许部署账号读取。
- 开发库、测试库和生产库使用不同账号及不同密码。
- 生产数据库必须与共享开发和测试数据库隔离。
- Preview HMAC Secret 不得进入前端、数据库业务表或日志；部署时使用独立随机值。
- 启用 Batch Worker 前必须同时完成 Provider 配置并显式设置
  `INFORMATION_HUB_AI_BATCH_WORKER_ENABLED=true`；默认关闭可避免意外产生模型费用。

- Recommendation Worker 默认关闭；需要消费手动或自动创建的 `PENDING` Run 时显式设置
  `INFORMATION_HUB_RECOMMENDATION_WORKER_ENABLED=true`。它不调用 Provider，但会写入 Recommendation Run/Item。

## 8. Token 与 HMAC Secret 生成示例

Collector Token 和 Preview HMAC Secret 都应使用密码学安全随机数自行生成。以下 PowerShell
示例生成 32 个随机字节并编码为 Base64；命令输出就是候选秘密，请只保存到环境变量、受控
`config/application.yml` 或部署 Secret，不要粘贴到聊天、日志或 Git：

```powershell
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$generator.GetBytes($bytes)
$token = [Convert]::ToBase64String($bytes)
$generator.Dispose()
$token
```

- 可将不同运行结果分别用于 `INFORMATION_HUB_COLLECTOR_TOKEN` 和
  `INFORMATION_HUB_PREVIEW_HMAC_SECRET`，两者不要复用；
- Bootstrap 密码应由密码管理器生成并单独保存；
- Provider API Key 不能用上述脚本代替，必须从 Provider 官方控制台创建。