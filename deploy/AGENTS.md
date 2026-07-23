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

## 四、数据持久化

- MySQL 使用持久化卷。
- 普通脚本不得执行 `docker compose down -v`。
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
- 日志是否泄露敏感信息。
