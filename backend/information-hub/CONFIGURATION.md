# Information Hub 配置说明

## 1. 配置范围

本文件说明 Information Hub Spring Boot 服务端的数据库、Flyway、Collector Token 和请求体上限配置。

Collector 如何连接 Information Hub，参见 [BOSS Collector 集成说明](../../collectors/boss-zhipin-scraper/INTEGRATION.md)。

## 2. 创建本地配置文件

Spring Boot 原生支持从外部 `config/application.yml` 加载配置。先在仓库根目录复制示例模板：

```powershell
Copy-Item backend/information-hub/config/application.yml.example backend/information-hub/config/application.yml
```

然后修改 `backend/information-hub/config/application.yml` 中的数据库连接、数据库密码和 Collector Token。

真实 `application.yml` 已被 Git 忽略，仓库只提交不包含真实秘密的 `application.yml.example`。

## 3. 本地启动

### 3.1 建立远程 MySQL 的 SSH 隧道

当前共享开发数据库运行在远程服务器的 Docker MySQL 容器中。MySQL 宿主机端口只绑定远程服务器的 `127.0.0.1:3306`，不直接向公网开放，因此本地运行 Information Hub 时必须先建立 SSH 隧道。

已按部署说明配置本机 OpenSSH 别名后，在单独的 PowerShell 窗口中运行：

```powershell
ssh -N -L 13306:127.0.0.1:3306 information-platform-server
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

## 5. 配置项

示例模板包含：

- `spring.datasource`：MySQL URL、用户名和密码。
- `spring.flyway`：迁移开关、迁移目录和安全选项。
- `information-hub.collector-api.token`：Collector 调用接入 API 时使用的 Bearer Token。
- `information-hub.collector-api.max-request-bytes`：Collector 单次请求体的字节上限。
- `mybatis-plus.configuration`：MyBatis-Plus 基础映射配置。

Collector 中配置的 `collector_token` 必须与服务端的 `information-hub.collector-api.token` 一致。

## 6. 环境变量覆盖

环境变量仍可按 Spring Boot 配置规则覆盖 YAML，适合 CI、自动化部署或临时调整。当前应用内置配置使用以下变量：

```text
INFORMATION_HUB_DB_URL
INFORMATION_HUB_DB_USERNAME
INFORMATION_HUB_DB_PASSWORD
INFORMATION_HUB_FLYWAY_ENABLED
INFORMATION_HUB_COLLECTOR_TOKEN
INFORMATION_HUB_COLLECTOR_MAX_REQUEST_BYTES
```

## 7. 安全要求

- 不要把真实数据库密码、Token、Cookie 或服务器秘密写入 `.example` 文件。
- 服务器上的 `application.yml` 应只允许部署账号读取。
- 开发库、测试库和生产库使用不同账号及不同密码。
- 生产数据库必须与共享开发和测试数据库隔离。

## 8. token生成方式
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $bytes = New-Object byte[] 32
    $generator.GetBytes($bytes)
    $token = [Convert]::ToBase64String($bytes)
    $generator.Dispose()
    $token
