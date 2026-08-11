# Information Hub Web

Information Platform 的标准化信息浏览前端。Phase 2 职位浏览、Phase 3 Identity/AI 页面和 Phase 4 Recommendation Profile、Feed、Refresh、Feedback、JOB contact status 与真实全栈 E2E 均已完成。

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

- `http://localhost:5173/login`：账号登录
- `http://localhost:5173/jobs`：职位列表
- `http://localhost:5173/jobs/{id}`：职位详情
- `http://localhost:5173/jobs/{id}/snapshots`：历史快照列表和版本内容
- `http://localhost:5173/recommendations`：职位推荐画像、Feed、刷新与交互
- `http://localhost:5173/ai/prompts`：Prompt Profile 与版本管理
- `http://localhost:5173/ai/analyze`：手动 Analysis Preview 与确认
- `http://localhost:5173/ai/analyses/{id}`：Analysis 结果
- `http://localhost:5173/ai/batches`：Analysis Batch 列表
- `http://localhost:5173/ai/batches/{id}`：Analysis Batch 详情
- `http://localhost:5173/ai/schedules`：Analysis Schedule
- `http://localhost:5173/ai/usage`：Actual Token Usage
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

`src/api` 统一导出 Job Query 与 Recommendation 类型化函数。Recommendation 写请求通过同源 Session 和 CSRF 调用 Profile、Refresh、View、Feedback 与 JOB disposition Contract。

Job Query 只读函数：

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

`test:e2e` 使用本地稳定 Fixture 拦截 Job Query 与 Recommendation API，不访问远程 MySQL，不读取或提交真实职位数据。除既有职位场景外，还覆盖 Profile save、Feed/score/reasons、Viewed、Refresh polling、Feedback、CONTACTED 保留、hard exclusion 即时隐藏、undo、stale 与错误态。

Phase 4 真实全栈验收使用：

```powershell
npm.cmd run test:e2e:phase4
```

该命令需要先构建后端 JAR，并设置 `INFORMATION_HUB_E2E_DB_URL/USERNAME/PASSWORD`，也可复用 `INFORMATION_HUB_TEST_DB_URL/USERNAME/PASSWORD`。数据库名必须包含 `test` 或 `e2e` 且不得包含 `dev`。运行器生成一次性账号和所有测试秘密，只在测试源集中写入密码摘要；随后启动真实 Information Hub、Fake Provider、Vite 与 Chromium，验证 Analysis Batch → Recommendation 全链路。

职位浏览覆盖：

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

## Phase 3 全栈 E2E

TASK-032 使用真实 Web、真实 Information Hub、专用 MySQL 测试库和本机 Fake Provider：

```powershell
npm.cmd run test:e2e:phase3
```

运行前需设置 `INFORMATION_HUB_E2E_DB_URL/USERNAME/PASSWORD`、
`INFORMATION_HUB_E2E_USERNAME/PASSWORD`、`INFORMATION_HUB_E2E_COLLECTOR_TOKEN`
和 `INFORMATION_HUB_E2E_PREVIEW_HMAC_SECRET`。数据库名称必须包含 `test` 或 `e2e`，
且不得包含 `dev`；HMAC Secret 至少 32 个 UTF-8 字节。后端可执行 JAR 需先通过
`backend/information-hub` 下的 `mvnw -DskipTests package` 构建。

该命令只使用合成 JOB 和运行时随机 Fake Provider Key，覆盖登录、Prompt、单条分析、
Preview、Item/Token Budget、Manual Batch、失败 Usage、Schedule 触发和用户 Usage；
验收完成后临时 Schedule 会在 `finally` 中关闭。真实 Provider Key 只允许进入服务端环境变量，
不得放入前端 `.env`、Git 或浏览器。
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
