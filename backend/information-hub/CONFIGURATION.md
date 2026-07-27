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

从 Information Hub 模块目录启动：

```powershell
Set-Location backend/information-hub
.\mvnw.cmd spring-boot:run
```

Spring Boot 会读取当前模块下的 `config/application.yml`。

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
