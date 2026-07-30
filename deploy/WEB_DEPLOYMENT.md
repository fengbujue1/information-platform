# Information Hub Web 受控部署

本文说明 TASK-018 提供的前端生产构建和 Nginx 同源代理模板。该模板默认只绑定宿主机回环地址，不是公网生产发布方案。

## 拓扑和安全边界

```text
受控浏览器
  -> 127.0.0.1:18080
  -> Nginx
       -> Vue 静态文件
       -> /api/* -> Information Hub
```

- Web 容器通过 Compose 的 `web` profile 显式启用。
- 宿主机端口默认绑定 `127.0.0.1:18080`，不允许匿名公网访问。
- 浏览器始终请求同源 `/api`，Nginx 再转发到 Information Hub。
- Nginx 不添加通配 CORS 响应头。
- 前端镜像不包含 Collector Token、数据库密码、SSH 密钥或其他秘密。
- 需要远程访问时使用 SSH 隧道或受控网络；域名、TLS 和用户认证不属于本任务。

## 配置

复制部署模板并只在本机或服务器保存真实 `.env`：

```powershell
Copy-Item deploy\.env.example deploy\.env
```

Web 相关变量：

```dotenv
WEB_HOST_PORT=18080
INFORMATION_HUB_UPSTREAM=http://host.docker.internal:8080
```

`WEB_HOST_PORT` 是宿主机回环端口。`INFORMATION_HUB_UPSTREAM` 是 Nginx 容器能够访问的 Information Hub 地址，不会进入浏览器构建产物。默认值适合 Information Hub 运行在 Docker 宿主机的 `8080` 端口。

在 Linux Docker 中，Compose 通过 `host-gateway` 提供 `host.docker.internal`。Information Hub 必须监听容器能够访问的宿主机地址，但对应端口仍应由防火墙阻止公网入站；不要为了连通容器直接开放未认证 API。

## 校验和启动

从仓库根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\web\tests\validate-web-compose.ps1
docker compose --env-file deploy\.env --file deploy\docker-compose.yml --profile web build web
docker compose --env-file deploy\.env --file deploy\docker-compose.yml --profile web up -d --no-deps web
docker compose --env-file deploy\.env --file deploy\docker-compose.yml --profile web ps
```

只验证静态配置、不调用 Docker：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\web\tests\validate-web-compose.ps1 -StaticOnly
```

## 健康和功能探针

Nginx 健康检查不依赖数据库或 Information Hub：

```powershell
Invoke-RestMethod http://127.0.0.1:18080/healthz
```

预期返回 `ok`。

SPA 深层路由必须返回前端入口，而不是 Nginx 404：

```powershell
(Invoke-WebRequest http://127.0.0.1:18080/jobs/7/snapshots).Content |
    Select-String '<div id="app"></div>'
```

Information Hub 可访问且已有数据时，再执行只读 API 探针：

```powershell
Invoke-RestMethod 'http://127.0.0.1:18080/api/v1/jobs?page=1&size=1'
```

API 探针失败但 `/healthz` 成功，通常表示 `INFORMATION_HUB_UPSTREAM` 不可达、Information Hub 未启动或其数据库连接异常。先在容器宿主机验证 Information Hub，再检查 Nginx 容器到上游的网络路径。

## 远程受控访问

Web 端口保持绑定服务器 `127.0.0.1`。开发电脑通过 SSH 转发：

```powershell
ssh -N -L 18080:127.0.0.1:18080 information-platform-server
```

隧道保持运行时，在本机访问：

```text
http://127.0.0.1:18080/jobs
```

不得在云安全组或服务器防火墙中向公网开放 `18080`，也不得把 Compose 端口映射改为 `0.0.0.0`，除非后续独立任务同时提供认证、TLS、网络访问控制和安全验收。

## 停止

只停止 Web：

```powershell
docker compose --env-file deploy\.env --file deploy\docker-compose.yml --profile web stop web
```

删除 Web 容器但保留 MySQL 数据卷：

```powershell
docker compose --env-file deploy\.env --file deploy\docker-compose.yml --profile web rm --stop --force web
```

不要执行 `docker compose down -v`，该命令会删除项目数据卷。
