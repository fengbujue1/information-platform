# TASK-004A：建立远程 MySQL 开发与测试环境

状态：TODO
所属阶段：Phase 1
优先级：P0

## 前置依赖

- TASK-003 完成。
- ADR-008 已接受。
- 已有可通过 SSH 访问的远程服务器。
- 远程服务器已安装 Docker 和 Docker Compose。

## 后续依赖

- TASK-004 必须在本任务完成后执行。
- TASK-005 继续依赖 TASK-004，不直接依赖本任务。

## 目标

在远程服务器中建立可重复部署、安全连接并持久化的 MySQL 开发与集成测试环境，使不同开发主机使用同一套受控数据库环境。

## 交付物

- `deploy/mysql/docker-compose.yml`
- `deploy/mysql/.env.example`
- `deploy/mysql/README.md`
- 必要的数据库和账号初始化脚本

初始化脚本只允许创建数据库、账号和权限，不创建 Phase 1 业务表。

## 实施范围

- 使用 Docker Compose 创建 MySQL 容器。
- 固定明确的 MySQL 8.4 LTS 补丁版本，禁止使用 `latest`。
- 配置容器健康检查、重启策略和持久化数据卷。
- 创建相互隔离的开发库和集成测试库。
- 为开发库和测试库创建不同的非 root 最小权限账号。
- MySQL 宿主机端口只绑定远程服务器的 `127.0.0.1`。
- 提供从不同开发主机建立 SSH 隧道的可执行命令。
- 提供容器创建、启动、停止、状态检查、备份和恢复命令。
- 提供不含真实密码的环境变量模板。

## 安全要求

- 不向公网直接开放 MySQL `3306` 端口。
- 服务器防火墙或安全组不得允许公网入站访问 MySQL 端口。
- SSH 隧道使用密钥认证，私钥只保存在开发主机。
- 不在 Git 中保存真实数据库密码、服务器地址、SSH 私钥或 TLS 私钥。
- Information Hub 不使用 MySQL root 账号。
- 开发账号不得访问测试库，测试账号不得访问开发库。
- 自动测试不得连接开发库。
- 禁止在共享开发或测试环境执行 `flyway clean`。
- 日志和操作示例不得输出真实凭据。

## 验收标准

- [ ] `docker compose config` 校验通过。
- [ ] 使用文档命令可以创建并启动 MySQL 容器。
- [ ] 容器健康检查通过。
- [ ] MySQL 镜像使用固定版本。
- [ ] MySQL 端口只绑定远程服务器回环地址。
- [ ] 从公网无法直接连接 MySQL 端口。
- [ ] SSH 隧道使用密钥认证。
- [ ] 两台开发主机均可通过 SSH 隧道执行 `SELECT 1`。
- [ ] 开发库和测试库相互隔离。
- [ ] 开发账号和测试账号权限符合最小权限要求。
- [ ] 容器重启后验证数据仍然存在。
- [ ] 备份和恢复流程完成一次验证。
- [ ] Git 中不存在真实密码、Token、私钥或其他连接凭据。
- [ ] 未创建 Phase 1 业务表。

## 验证方法

- 执行 `docker compose config`。
- 执行 `docker compose up -d` 和 `docker compose ps`。
- 检查容器健康状态和 MySQL 版本。
- 分别使用开发账号和测试账号验证允许及拒绝的权限。
- 从两台开发主机通过 SSH 隧道执行连接测试。
- 从服务器外部验证 MySQL 端口未向公网开放。
- 重启容器后验证持久化测试数据。
- 执行一次备份和恢复演练。
- 执行 `git diff --check` 和敏感信息检查。

## 不在范围

- 不创建 `information_item`、`job_information` 或 `information_snapshot`。
- 不编写 Flyway 业务迁移。
- 不实现 Information Hub 接入 API。
- 不修改 Information Hub 业务代码。
- 不修改 BOSS Collector。
- 不部署生产数据库。
- 不配置 MySQL 主从复制、高可用或数据库代理。
- 不引入 Redis、Kafka、RabbitMQ、MongoDB 或 Kubernetes。
- 不向公网直接开放 MySQL。
- 不建设公网直连所需的 TLS 证书体系。
- 不建设 CI/CD、数据库监控或集中日志平台。
