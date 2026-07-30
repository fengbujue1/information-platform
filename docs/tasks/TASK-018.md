# TASK-018：建立 CI 与受控构建部署配置

状态：DONE
所属阶段：Phase 2
优先级：P1
负责人：User + Codex

## 1. 目标

建立基础持续集成，并提供不会把无认证读取 API 直接暴露公网的前端构建和同源代理方案。

## 2. 背景

Phase 1 和 Phase 2 已包含 Java、Python 和 Vue 三类代码。仓库需要统一的自动检查。

前端部署还需要解决静态文件、深层路由和 `/api` 代理。

## 3. 前置依赖

- TASK-017 已完成。
- 所有前端 npm scripts 已稳定。

## 4. 影响范围

- `.github/workflows/`
- 前端构建配置
- `deploy/`
- Nginx 配置模板
- `.env.example`
- 部署文档
- TASK 和 CURRENT_STATUS

## 5. 本任务范围

CI 至少运行：

- Java 21 Maven 测试；
- Python Collector 测试；
- 前端 `npm ci`；
- 前端 typecheck；
- 前端测试；
- 前端 build；
- `git diff --check`；
- 基础敏感文件检查。

构建部署至少提供：

- 前端生产构建；
- SPA 深层路由回退；
- `/api` 同源反向代理模板；
- 配置模板；
- 健康检查说明；
- 受控访问说明。

## 6. 不在本任务范围

- 不部署到公网。
- 不配置域名和证书，除非用户单独创建部署任务。
- 不增加用户认证。
- 不部署生产数据库。
- 不引入 Kubernetes。
- 不重写现有 MySQL 部署。
- 不在 CI 中连接私人远程开发库。

## 7. 业务与技术规则

- CI 不依赖私人 SSH 密钥。
- CI 不依赖远程共享 MySQL。
- 数据库条件测试保持安全跳过，或后续单独引入 Testcontainers。
- 所有秘密使用 GitHub Secrets，仓库不保存真实值。
- Nginx 不添加 `Access-Control-Allow-Origin: *`。
- 未认证环境默认只允许本机或受控网络访问。

## 8. 验收标准

- [x] Java 测试工作流已配置并通过同命令本地验证
- [x] Python 测试工作流已配置并通过同命令本地验证
- [x] 前端 typecheck、test、build 工作流已配置并通过同命令本地验证
- [x] SPA 深层路由可刷新
- [x] `/api` 同源代理配置可校验
- [x] 没有真实秘密进入 Git
- [x] 没有公网无认证部署
- [x] 部署文档明确访问边界
- [x] CURRENT_STATUS 更新为 TASK-019

## 9. 实施前计划

Codex 必须先列出：

- 工作流拆分
- 运行命令
- 缓存策略
- Nginx 拓扑
- 不可在 CI 使用的秘密和远程资源
- 验证方法

以上计划已在实施前汇报并经用户确认。

## 10. 实施记录

- 新增 GitHub Actions `CI` 工作流，拆分 repository checks、backend、collector、frontend 和 web deployment 五个 job。
- 固定 `actions/checkout@v6`、`actions/setup-java@v5`、`actions/setup-python@v6` 和 `actions/setup-node@v6`；Java 使用 Temurin 21，Node 使用前端 `.nvmrc`，Python 使用 Collector 声明的最低版本 3.10。
- Maven、pip 和 npm 仅使用各自官方 Action 缓存，不缓存构建输出。
- CI 不读取 SSH 密钥、远程 MySQL 或私人配置；数据库条件集成测试缺少专用环境变量时按现有逻辑安全跳过。
- 新增多阶段前端镜像：固定 Node.js 24.18.0 Alpine 构建，固定 Nginx 1.30.4 Alpine 运行。
- 在项目级 Compose 增加可选 `web` profile，不改变默认 MySQL 启动集合。
- Web 端口默认只绑定 `127.0.0.1:18080`，通过 Nginx 提供 `/healthz`、SPA 深层路由回退和 `/api` 同源代理。
- 使用 `host-gateway` 支持容器访问宿主机 Information Hub；未开放公网端口，未添加通配 CORS。
- 新增静态及 Compose 渲染校验脚本、Docker 构建上下文忽略文件、部署配置模板和受控访问文档。
- 扩展仓库检查脚本，校验 Phase 2 必需文档、私有配置、运行时目录、常见秘密文件扩展名和私钥头。
- 未修改 Java、Python、数据库结构、前端业务页面或 Collector。

## 11. 测试结果

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-project.ps1`：通过。
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\web\tests\validate-web-compose.ps1 -StaticOnly`：通过。
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\web\tests\validate-web-compose.ps1`：通过，Compose 渲染后的 Web profile、回环端口和健康检查符合约束。
- 在 JDK 21.0.11 下执行 `.\mvnw.cmd test`：通过，46 个测试，0 失败，13 个远程数据库条件测试安全跳过。
- 设置 `PYTHONUTF8=1` 后执行 `python -m unittest discover -s tests -p "test_*.py"`：通过，144 个测试。
- `npm.cmd ci --no-audit --no-fund`：通过，按 lockfile 安装 205 个包。
- `npm.cmd run typecheck`：通过。
- `npm.cmd run test`：通过，14 个测试文件、62 个测试。
- `npm.cmd run build`：通过，生成生产静态文件。
- 使用本机已有 Nginx 容器挂载本次模板和生产构建执行 `nginx -t`：通过。
- `GET /healthz`：返回 `ok`。
- `GET /jobs/7/snapshots`：返回 200 且命中 Vue 入口，SPA 深层路由回退通过。
- 本机固定版本镜像构建尝试失败：Docker Desktop 配置的 `registry.docker-cn.com` 镜像源返回 EOF。该问题不涉及仓库配置；标准 GitHub Runner 上的 `web-deployment` job 已覆盖固定镜像构建与容器探针。
- `git diff --check`：通过。

## 12. 遗留问题

- 工作流只有在本次修改提交并 push 后才能由 GitHub Actions 实际执行。
- 本机若要复验固定版本镜像构建，需要先修复或移除失效的 Docker Registry Mirror；不要为绕过该问题修改项目固定镜像版本。
- TASK-019 负责真实浏览器、Web、Information Hub、Job Query API 和 MySQL 的 Phase 2 端到端验收。

## 13. 完成确认

- [x] CURRENT_STATUS 已更新
- [ ] 用户已检查 git diff
- [ ] 用户已确认测试结果
- [ ] 已提交并 push
