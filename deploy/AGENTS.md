# Deploy 开发规则

## 一、模块定位

`deploy/` 保存本地和部署配置，当前优先使用 Docker Compose。

## 二、当前允许组件

- MySQL
- Information Hub Backend
- Information Hub Web
- Nginx

BOSS Collector 当前继续运行在 Windows 宿主机。

未经 TASK 批准，不引入 Kafka、MongoDB、Elasticsearch、Kubernetes 或服务发现。

## 三、安全

不得提交：

- 真实数据库密码；
- AI API Key；
- Collector Token；
- Cookie；
- TLS 私钥；
- Chrome Profile。

只提交 `.env.example`。

远程 MySQL 还必须遵守：

- 宿主机端口只绑定远程服务器的 `127.0.0.1`。
- 不直接向公网开放 MySQL `3306`。
- 服务器防火墙或安全组不得允许公网入站访问 MySQL 端口。
- 开发主机默认通过使用密钥认证的 SSH 隧道连接。
- SSH 私钥只保存在开发主机，不得放入部署目录。
- 应用和测试不得使用 MySQL root 账号。
- 开发库与测试库使用不同的最小权限账号。
- 自动测试不得连接开发库。
- 日志和命令示例不得输出真实凭据。

## 四、数据持久化

- MySQL 使用持久化卷。
- MySQL 镜像必须固定明确版本，不使用 `latest`。
- 普通脚本不得执行 `docker compose down -v`。
- 共享开发或测试环境不得执行 `flyway clean`。
- 快照表属于业务历史，备份策略必须覆盖该表。
- 破坏性清理必须明确提示。

## 五、验证

修改 Compose 后至少执行：

```powershell
docker compose config
docker compose up -d
docker compose ps
```

并检查：

- 数据库连接；
- Flyway；
- 健康状态；
- 持久化卷；
- 开发库和测试库的账号隔离；
- SSH 隧道连接；
- 备份和恢复；
- 日志是否泄露敏感信息。
