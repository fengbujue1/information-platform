# TASK-004A：建立远程 MySQL 开发与测试环境

状态：DONE
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

- `deploy/docker-compose.yml`
- `deploy/.env.example`
- `deploy/README.md`
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

- [x] `docker compose config` 校验通过。
- [x] 使用文档命令可以创建并启动 MySQL 容器。
- [x] 容器健康检查通过。
- [x] MySQL 镜像使用固定版本。
- [x] MySQL 端口只绑定远程服务器回环地址。
- [x] 从公网无法直接连接 MySQL 端口。
- [x] SSH 隧道使用密钥认证。
- [x] 当前开发主机可通过 SSH 隧道执行 `SELECT 1`。
- [x] 开发库和测试库相互隔离。
- [x] 开发账号和测试账号权限符合最小权限要求。
- [x] 容器重启后验证数据仍然存在。
- [x] 备份和恢复流程完成一次验证。
- [x] Git 中不存在真实密码、Token、私钥或其他连接凭据。
- [x] 未创建 Phase 1 业务表。

## 验证方法

- 执行 `docker compose config`。
- 执行 `docker compose up -d` 和 `docker compose ps`。
- 检查容器健康状态和 MySQL 版本。
- 分别使用开发账号和测试账号验证允许及拒绝的权限。
- 从当前开发主机通过 SSH 隧道执行连接测试。
- 从服务器外部验证 MySQL 端口未向公网开放。
- 重启容器后验证持久化测试数据。
- 执行一次备份和恢复演练。
- 执行 `git diff --check` 和敏感信息检查。

## 实施记录

完成时间：2026-07-24

- 将 `deploy/docker-compose.yml` 作为项目级 Compose 入口，当前只声明 MySQL 服务，为后续后端、Web 和 Nginx 保留同一编排入口。
- 固定使用官方 `mysql:8.4.10` 镜像，配置 UTC、utf8mb4、健康检查、`unless-stopped` 和命名持久化卷。
- MySQL 宿主机端口只绑定远程服务器 `127.0.0.1:3306`，未开放公网监听。
- 在远程服务器本地生成私有 `.env`，权限为 `600`，真实密码未进入 Git 或命令日志。
- 创建 `information_hub_dev` 和 `information_hub_test`，分别使用不同的非 root、数据库级权限账号。
- 增加初始化脚本、Windows Compose 静态验证脚本、远程集成验证脚本、SSH 隧道示例及部署运维文档。
- 远程服务器无法直接访问 Docker Hub 时，从当前开发主机拉取官方镜像，校验传输归档 SHA-256 后导入；本机和服务器镜像 ID 一致。
- 未创建 Phase 1 业务表，未修改 Information Hub 业务代码或 BOSS Collector。

## 验证结果

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\mysql\tests\validate-compose.ps1`：通过。
- 远程 `docker compose --env-file .env --file docker-compose.yml config --quiet`：通过。
- 远程 `bash mysql/tests/verify-remote.sh`：通过。
- MySQL 版本：8.4.10；容器状态：healthy。
- 端口监听：仅 `127.0.0.1:3306`；从当前开发主机测试公网 `3306` 不可达。
- 当前开发主机通过 SSH 隧道执行 `SELECT 1`：通过。
- 开发账号和测试账号跨库访问拒绝：通过。
- 显式事务写入、容器重建后的数据持久化、临时探针备份恢复：通过。
- `information_item`、`job_information`、`information_snapshot` 和临时探针残留数量：0。
- 远程 `.env` 权限：600；日志敏感标记检查：未发现。

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
