# Information Hub Web

Information Platform 的标准化信息浏览前端。当前已实现 Vue 3 工程骨架、强类型 Job Query API Client、真实职位列表、职位详情和历史快照查看。

## 环境要求

- Node.js 24.18.0
- npm 11.16.0

使用 nvm-windows 时，可根据 `.nvmrc` 中的版本执行：

```powershell
nvm install 24.18.0
nvm use 24.18.0
```

## 本地运行

```powershell
npm ci
npm run dev
```

如果 PowerShell 因执行策略禁止运行 `npm.ps1`，可直接使用 Windows 命令入口，无需放宽系统执行策略：

```powershell
npm.cmd ci
npm.cmd run dev
```

开发服务器启动后访问：

- `http://localhost:5173/jobs`：职位列表
- `http://localhost:5173/jobs/{id}`：职位详情
- `http://localhost:5173/jobs/{id}/snapshots`：历史快照列表和版本内容
- 任意不存在的前端路径将显示 404 页面

## API 开发配置

复制 `.env.example` 为不提交 Git 的 `.env.local` 后，可以覆盖以下非敏感配置：

```dotenv
VITE_API_BASE_URL=/api
INFORMATION_HUB_PROXY_TARGET=http://127.0.0.1:8080
```

- `VITE_API_BASE_URL` 是浏览器使用的同源 API 前缀，默认值为 `/api`。
- `INFORMATION_HUB_PROXY_TARGET` 仅由 Vite 开发服务器读取，默认指向本机 `8080` 端口的 Information Hub。
- Vite 保留 `/api` 前缀，将 `/api/v1/jobs` 等请求代理到 Information Hub。

如果 Information Hub 通过 SSH 隧道映射到其他本地端口，只需在 `.env.local` 修改 Proxy Target。不要在任何 `VITE_` 变量中保存密码、Token、Cookie 或数据库连接信息。

## API Client

`src/api` 统一导出以下只读函数：

- `getJobs`
- `getJobById`
- `getJobSnapshots`

页面必须通过这些函数读取数据，不直接创建 Axios 实例。API 错误统一转换为 `ApiClientError`，不会向页面暴露 Axios 内部 request、response 或 config。

`/jobs` 通过 `getJobs` 加载真实职位列表，`/jobs/:id` 通过 `getJobById` 加载当前详情，`/jobs/:id/snapshots` 通过 `getJobSnapshots` 加载按后端顺序排列的历史版本。

## 生产构建与受控部署

本地只生成生产静态文件：

```powershell
npm run build
```

项目级 Compose 提供可选的 `web` profile，使用固定版本 Node.js 构建静态文件，再由 Nginx 提供 SPA 深层路由回退和 `/api` 同源代理。该配置默认只绑定宿主机 `127.0.0.1:18080`，不属于公网发布方案。

完整配置、健康检查、SSH 隧道访问和停止方式见：

```text
deploy/WEB_DEPLOYMENT.md
```

## 验证命令

```powershell
npm run typecheck
npm run test
npm run build
```

PowerShell 存在上述执行策略限制时，将命令中的 `npm` 替换为 `npm.cmd`。

`.env.example` 只包含非敏感的本地开发示例。只有 `VITE_` 前缀变量会进入浏览器；任何前端或 Vite 环境变量都不得保存密码、Token 或 Cookie。
