# Information Hub Web

Information Platform 的标准化信息浏览前端。Phase 2 已完成职位列表、筛选、排序、分页、职位详情、历史快照、页面状态、受控构建部署和端到端验收。

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

如果 PowerShell 因执行策略禁止运行 `npm.ps1`，可直接使用 Windows 命令入口：

```powershell
npm.cmd ci
npm.cmd run dev
```

开发服务器启动后访问：

- `http://localhost:5173/jobs`：职位列表
- `http://localhost:5173/jobs/{id}`：职位详情
- `http://localhost:5173/jobs/{id}/snapshots`：历史快照列表和版本内容
- 任意不存在的前端路径：404 页面

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

列表、详情和快照页面不请求或展示 rawPayload，也不保存 Collector Token。

## 自动化验证

首次运行浏览器 E2E 前安装与项目版本匹配的 Chromium：

```powershell
npx playwright install chromium
```

完整前端验证：

```powershell
npm run typecheck
npm run test
npm run test:e2e
npm run build
```

`test:e2e` 使用本地稳定 Fixture 拦截 Job Query API，不访问远程 MySQL，不读取或提交真实职位数据。覆盖：

- 列表加载；
- 筛选和排序 URL；
- 分页；
- 列表进入详情；
- 详情进入快照；
- 深层路由刷新；
- 空数据；
- API 错误；
- 404；
- rawPayload 和 Collector Token 不展示。

PowerShell 存在执行策略限制时，将命令中的 `npm` 和 `npx` 分别替换为 `npm.cmd` 和 `npx.cmd`。

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

`.env.example` 只包含非敏感的本地开发示例。只有 `VITE_` 前缀变量会进入浏览器；任何前端或 Vite 环境变量都不得保存密码、Token 或 Cookie。
