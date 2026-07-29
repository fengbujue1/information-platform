# Information Hub Web

Information Platform 的标准化信息浏览前端。当前仅包含 Vue 3 工程骨架、`/jobs` 占位页和 404 占位页，不调用真实 API。

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

- `http://localhost:5173/jobs`
- 任意不存在的前端路径将显示 404 占位页

## 验证命令

```powershell
npm run typecheck
npm run test
npm run build
```

PowerShell 存在上述执行策略限制时，将命令中的 `npm` 替换为 `npm.cmd`。

`.env.example` 只用于说明可公开到浏览器的非敏感环境变量。不要在前端环境变量中保存密码、Token 或 Cookie。
