# Deploy 开发规则

## 一、模块定位

`deploy/` 保存 Information Platform 的本地和部署配置。

当前主要使用 Docker Compose。

## 二、当前部署范围

当前允许包含：

- MySQL
- Information Hub Backend
- Information Hub Web
- Nginx

BOSS 采集器当前可以继续运行在 Windows 宿主机，因为它依赖真实 Chrome、CDP 和人工登录。

除非当前 TASK 明确要求，不引入：

- Kubernetes
- Kafka
- MongoDB
- Elasticsearch
- Redis 集群
- 服务发现和配置中心

## 三、安全规则

不得提交：

- 真实数据库密码；
- AI API Key；
- Information Hub Token；
- Cookie；
- TLS 私钥；
- Chrome Profile。

Compose 文件使用环境变量：

``yaml
environment:
  MYSQL_PASSWORD: ${MYSQL_PASSWORD}

仓库只保留：

.env.example
##四、数据持久化
MySQL 必须使用持久化卷。
删除容器不应默认删除数据卷。
不得在普通启动脚本中执行 docker compose down -v。
破坏性清理命令必须明确提示。
备份和恢复操作必须有文档。
##五、端口规则

端口需要集中记录，不要在多个文件中随意变化。

建议由实际配置确认后记录：

MySQL：
Backend：
Frontend：
Nginx：

不得在没有检查端口占用的情况下自行更换公共端口。

##六、验证命令

修改 Compose 后至少执行：

docker compose config
docker compose up -d
docker compose ps

还需要检查：

容器健康状态；
数据库连接；
后端启动；
持久化卷；
日志中是否泄露敏感信息。
##七、完成任务前

汇报：

修改的服务；
新增环境变量；
启动命令；
验证结果；
数据迁移风险；
回滚方法。