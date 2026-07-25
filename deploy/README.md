# Information Platform 部署

deploy/ 是 Information Platform 的项目级 Compose 入口。TASK-004A 只在其中部署共享开发和集成测试 MySQL；后续任务可以在同一 Compose 中加入 Information Hub、Web 和 Nginx。当前不部署生产数据库，不创建 Phase 1 业务表，也不允许 Collector 直接连接数据库。

## 目录内容

- `docker-compose.yml`：固定版本的 MySQL Compose 配置。
- `.env.example`：可以提交的环境变量模板，不包含真实密码。
- `mysql/init/01-create-databases.sh`：首次初始化开发库、测试库及隔离账号。
- `mysql/examples/ssh-config.example`：每台开发电脑的 OpenSSH 配置示例。
- `mysql/tests/validate-compose.ps1`：在 Windows 上执行的静态配置测试。
- `mysql/tests/verify-remote.sh`：在远程服务器执行的集成验证。

## 安全边界

- MySQL 只绑定远程服务器的 `127.0.0.1`，不直接开放公网。
- 服务器防火墙和云安全组都不得允许公网入站访问 MySQL 端口。
- 开发电脑通过使用密钥认证的 SSH 隧道连接。
- 每台开发电脑使用不同的 SSH 密钥。
- 真实 `.env`、SSH 私钥、数据库备份和服务器连接信息不得提交 Git。
- Information Hub 使用开发账号或测试账号，不使用 root。
- 自动测试只连接测试库。
- 不得在共享环境执行 `flyway clean`。
- 不得执行 `docker compose down -v`。

## 一、新电脑配置 SSH 密钥

以下操作在每一台 Windows 开发电脑分别执行。不要通过 Git、网盘或聊天在电脑之间复制私钥。

### 1. 生成当前电脑专用密钥

在 PowerShell 执行：

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\.ssh" | Out-Null
ssh-keygen -t ed25519 -a 100 -f "$env:USERPROFILE\.ssh\information-platform-remote"
```

建议设置私钥密码。生成的文件为：

```text
C:\Users\当前用户名\.ssh\information-platform-remote
C:\Users\当前用户名\.ssh\information-platform-remote.pub
```

第一个文件是私钥，只能保存在当前电脑。第二个文件是可以安装到服务器的公钥。

### 2. 通过 Xshell 安装公钥

先在当前电脑查看公钥：

```powershell
Get-Content "$env:USERPROFILE\.ssh\information-platform-remote.pub"
```

使用现有的 Xshell 账号密码登录服务器，然后执行：

```bash
umask 077
mkdir -p ~/.ssh
chmod 700 ~/.ssh
nano ~/.ssh/authorized_keys
```

将完整公钥追加为新的一行，保存后执行：

```bash
chmod 600 ~/.ssh/authorized_keys
```

在确认密钥登录成功前，不要删除原有登录方式，也不要修改服务器的 SSH 密码登录策略。

### 3. 配置本机 OpenSSH

仓库中的示例文件是：

```text
deploy/mysql/examples/ssh-config.example
```

将其中的配置块合并到本机文件：

```text
C:\Users\当前用户名\.ssh\config
```

不要直接覆盖已有的 `config`。需要在本机修改：

- `HostName`
- `Port`
- `User`
- `IdentityFile`

真实配置只保存在本机，不要复制回仓库。

### 4. 加载带密码的私钥

如果私钥设置了密码，在自己的 PowerShell 中执行：

```powershell
Start-Service ssh-agent
ssh-add "$env:USERPROFILE\.ssh\information-platform-remote"
```

如果服务尚未启用，使用管理员 PowerShell 执行一次：

```powershell
Set-Service ssh-agent -StartupType Manual
Start-Service ssh-agent
```

### 5. 测试 SSH

```powershell
ssh information-platform-server
```

测试通过后退出：

```bash
exit
```

## 二、远程服务器首次部署

在服务器中进入仓库的 MySQL 部署目录：

```bash
cd /information-platform
```

实际路径由服务器管理员确定。不要把真实服务器路径硬编码到仓库。

### 1. 预检

```bash
docker --version
docker compose version
docker ps
ss -lnt
df -h
```

如果当前用户没有 Docker 权限，应由服务器管理员决定是否使用 `sudo`。不要自行修改 Docker 用户组或服务器权限。

确认没有需要保留的容器占用计划使用的宿主机端口。

### 2. 创建私有 `.env`

以下命令直接生成随机密码并写入 `.env`，不会把密码打印到终端：

```bash
umask 077
{
  printf '%s\n' \
    'COMPOSE_PROJECT_NAME=information-platform-mysql' \
    'MYSQL_HOST_PORT=3306' \
    'MYSQL_DATA_VOLUME=information-platform-mysql-data' \
    ''
  printf 'MYSQL_ROOT_PASSWORD='
  openssl rand -base64 36 | tr -d '\n'
  printf '\n\n'
  printf '%s\n' \
    'INFORMATION_HUB_DEV_DATABASE=information_hub_dev' \
    'INFORMATION_HUB_DEV_USER=information_hub_dev'
  printf 'INFORMATION_HUB_DEV_PASSWORD='
  openssl rand -base64 36 | tr -d '\n'
  printf '\n\n'
  printf '%s\n' \
    'INFORMATION_HUB_TEST_DATABASE=information_hub_test' \
    'INFORMATION_HUB_TEST_USER=information_hub_test'
  printf 'INFORMATION_HUB_TEST_PASSWORD='
  openssl rand -base64 36 | tr -d '\n'
  printf '\n'
} > .env
chmod 600 .env
```

验证文件权限，不要打印文件内容：

```bash
stat -c '%a %n' .env
```

预期权限为 `600`，并且 Git 应忽略该文件。

### 3. 校验并启动

不要把真实 Compose 配置输出保存到日志，因为渲染结果可能包含环境变量。

```bash
docker compose --env-file .env config --quiet
docker compose --env-file .env pull
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

检查 MySQL 版本：

```bash
docker compose --env-file .env exec -T mysql \
  sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --user=root --batch --skip-column-names --execute="SELECT VERSION();"'
```

必须返回 `8.4.10` 开头的版本号。

> 初始化脚本只会在空数据卷第一次启动时执行。修改 `.env` 或初始化脚本不会自动修改已有数据库账号。不得通过删除数据卷强制重新初始化。

## 三、建立 SSH 隧道

### Windows PowerShell

```powershell
ssh -N -L 13306:127.0.0.1:3306 information-platform-server
```

保持该窗口运行。应用和数据库客户端连接：

```text
主机：127.0.0.1
端口：13306
开发数据库：information_hub_dev
测试数据库：information_hub_test
```

不要把应用配置成直接连接服务器公网 IP。

停止隧道时在该 PowerShell 窗口按 `Ctrl+C`。

### Linux 或 macOS

```bash
ssh -N -L 13306:127.0.0.1:3306 information-platform-server
```

## 四、验证公网不可访问

在开发电脑 PowerShell 中设置实际服务器地址，然后执行：

```powershell
$serverAddress = "203.0.113.10"
Test-NetConnection -ComputerName $serverAddress -Port 3306
```

预期：

```text
TcpTestSucceeded : False
```

在服务器执行：

```bash
ss -lnt
```

MySQL 发布端口只能显示为 `127.0.0.1:3306`，不能显示为 `0.0.0.0:3306` 或 `[::]:3306`。

还必须在云厂商安全组中确认不存在允许公网访问 `3306` 的入站规则。仅绑定回环地址不能替代安全组检查。

## 五、测试

### 本机静态测试

在仓库根目录的 PowerShell 执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\mysql\tests\validate-compose.ps1
```

该测试使用 `.env.example`，不会读取真实 `.env`。

### 远程集成测试

必须在 TASK-004 创建业务表之前执行：

```bash
cd /information-platform
bash mysql/tests/verify-remote.sh
```

如果 Docker 必须通过 sudo 执行：

```bash
sudo bash mysql/tests/verify-remote.sh
```

该测试会：

- 校验 Compose。
- 启动 MySQL 并等待健康。
- 验证 MySQL 版本。
- 验证开发库和测试库连接。
- 验证两个账号不能跨库访问。
- 验证 Phase 1 业务表尚未创建。
- 在测试库创建临时探针表。
- 使用显式事务写入探针数据。
- 删除并重建容器但保留数据卷，验证数据仍然存在。
- 对临时探针表执行备份和恢复。
- 删除临时探针表和测试备份。

测试不会删除数据卷，不会连接或写入开发库业务数据。

### 第二台电脑验收

第二台电脑必须：

1. 生成自己的 SSH 密钥。
2. 将自己的公钥加入服务器。
3. 配置自己的 `.ssh/config`。
4. 建立本机 `13306` SSH 隧道。
5. 使用测试账号执行 `SELECT 1`。
6. 验证直接访问服务器 `3306` 失败。

## 六、手动备份和恢复

备份文件只能写入被 Git 忽略的 `deploy/mysql/backups/`，并应由服务器管理员转移到受保护的服务器外位置。

### 备份测试库

```bash
mkdir -p backups
chmod 700 backups
docker compose --env-file .env exec -T mysql \
  sh -c 'MYSQL_PWD="$INFORMATION_HUB_TEST_PASSWORD" mysqldump --single-transaction --skip-lock-tables --no-tablespaces --user="$INFORMATION_HUB_TEST_USER" "$INFORMATION_HUB_TEST_DATABASE"' \
  > backups/information-hub-test.sql
chmod 600 backups/information-hub-test.sql
```

### 恢复测试库

恢复会修改测试库，执行前必须确认目标环境：

```bash
docker compose --env-file .env exec -T mysql \
  sh -c 'MYSQL_PWD="$INFORMATION_HUB_TEST_PASSWORD" mysql --user="$INFORMATION_HUB_TEST_USER" "$INFORMATION_HUB_TEST_DATABASE"' \
  < backups/information-hub-test.sql
```

TASK-004A 不对开发库执行破坏性恢复测试。

## 七、安全停止与更新

停止但保留容器：

```bash
docker compose --env-file .env stop
```

删除容器和网络但保留命名数据卷：

```bash
docker compose --env-file .env down
```

禁止执行：

```text
docker compose down -v
docker volume rm information-platform-mysql-data
flyway clean
```

升级镜像版本必须通过独立任务修改 `docker-compose.yml`，完成备份并重新执行全部验证。

## 八、撤销某台电脑的访问权限

在服务器编辑：

```bash
nano ~/.ssh/authorized_keys
```

删除丢失或停用电脑对应的公钥行，然后确认权限：

```bash
chmod 600 ~/.ssh/authorized_keys
```

每台电脑使用独立密钥可以只撤销单台电脑，不影响其他开发电脑。

## 九、常见问题

### SSH 可以登录，但 Docker 无权限

不要直接修改用户组。由服务器管理员确认使用 `sudo`，或明确授予 Docker 权限。

### 修改 `.env` 后密码没有变化

初始化变量只在空数据卷首次启动时生效。不要删除数据卷。密码轮换需要使用明确的 MySQL `ALTER USER` 操作，并同步安全存储的连接配置。

### 本机 `13306` 已被占用

可以改用其他本机端口，例如：

```powershell
ssh -N -L 23306:127.0.0.1:3306 information-platform-server
```

远程 MySQL 端口仍保持回环绑定，不需要开放公网端口。

### 容器无法启动

```bash
docker compose --env-file .env ps
docker compose --env-file .env logs --tail 100 mysql
```

日志可能包含数据库元数据，排查后不要把未经检查的完整日志提交到仓库或公开聊天。

### mysql 密码位置
	服务器： /information-platform/.env