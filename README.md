# Information Platform
## 项目起源

现在的招聘平台并不缺职位，真正缺的是**高效的信息筛选**。

大公司和外包岗位反复刷屏、驻场职位混杂其中、部分 HR 长期不活跃，同一个或高度相似的职位还可能被算法反复推荐。为了找到真正适合自己的机会，往往需要每天花大量时间浏览、判断和跳过无效信息；有时今天刚排除的职位，第二天换一个名称又重新出现在推荐列表中。

`information-platform` 正是为了解决这个问题而诞生。

项目希望将**信息采集、归档、AI 分析、筛选、个性化推荐和浏览**整合到一个统一平台中，让系统承担大量重复的信息收集与筛选工作，把人的注意力真正留给值得关注的内容。

招聘职位只是项目的第一个落地场景。

`information-platform` 的核心并不绑定招聘业务，而是一套通用的信息收集与智能分析平台。未来可以通过统一的数据协议逐步扩展到新闻、政策、房价、教育等公开信息领域。

> **让机器负责收集和筛选海量信息，让人只关注真正有价值的部分。**

## AI 驱动的开发方式

这个项目从需求讨论、架构设计、数据库设计、任务拆解，到编码、测试、问题修复和阶段收尾，**全程采用 AI Coding 工具协作开发**。

与单纯“让 AI 写代码”不同，项目在开发过程中持续保留并维护完整的工程上下文，包括阶段规划、TASK、架构设计、数据库设计、ADR、API Contract、开发约束、实施记录和当前状态等文档，使重要的设计决策和开发过程不会只存在于某一次 AI 对话中。

因此，项目的长期上下文由 **Git 仓库 + 代码 + 文档**共同保存，而不是依赖某一个 AI 工具或某一段聊天记录。

即使更换电脑、更换 AI 对话，甚至更换 Codex 等不同的 AI Coding 工具，也可以让新的 AI 重新读取仓库中的代码和文档，快速恢复项目背景、当前阶段、既有约束和未完成任务，在现有基础上继续开发。

这也是本项目希望实践的一种开发方式：

> **AI 负责协助开发，文档负责保存上下文，Git 负责保存事实，让项目能够长期、连续地与不同 AI Coding 工具协作演进。**

## 当前进度

当前分支：`dev`

当前阶段：**Phase 5 — Product Refinement & Stabilization（进行中）**

| 阶段 | 状态 | 已交付能力 |
|---|---|---|
| Phase 0 | 已完成 | 项目初始化与开发基线 |
| Phase 1 | 已完成 | BOSS 采集、统一协议、MySQL 归档、Snapshot、查询 API |
| Phase 2 | 已完成 | Vue 3 职位浏览、筛选、详情、历史快照 |
| Phase 3 | 已完成 | Identity、Prompt、AI Analysis、Batch、Schedule、Token Usage |
| Phase 4 | 已完成 | 推荐画像、确定性评分、Feed、交互、自动触发 |
| Phase 5 | 进行中 | 产品体验、缺陷修复、联调与稳定性加固 |

Phase 5 采用 Rolling / Just-in-Time Task Planning，只为已经确认且可以独立验收的问题创建 TASK。

截至 2026-08-14：

- `TASK-045` 已完成 Phase 3 AI 页面域及推荐画像相关术语中文化；
- `TASK-046` 已完成全局操作反馈和浏览器公共 API 错误消息中文化；
- `TASK-047` 已完成 Analysis 批次限制配置化与预览结果文案澄清；
- `TASK-048` 已清除 `v0.1.0-beta.1` Baseline Candidate 的已知发布 blocker，本地自动化验证已通过，状态为 `VERIFYING`；
- 当前没有 Active / DEFERRED TASK；
- 下一个任务编号为 `TASK-049`；
- 仓库首个整体 Baseline 版本为 `v0.1.0-beta.1`；Backend 与 Frontend 组件版本为 `0.1.0-beta.1`；
- Collector 保持独立组件版本 `2.1.0`，不随仓库 Baseline 版本降级；
- 当前数据库结构仍为 Flyway `V4`：Generic Recommendation Core + JOB Extension。

最新事实以 [当前开发状态](docs/CURRENT_STATUS.md) 和 [Phase 5 Task Index](docs/PHASE5_TASK_INDEX.md) 为准。

## 系统链路

```text
BOSS 直聘
  ↓ 真实 Chrome / CDP
BOSS Collector
  ↓ InformationEnvelope V1 / HTTP
Information Hub
  ├─ 幂等归档与非破坏性合并
  ├─ Information / Job / Snapshot
  ├─ Identity / Session / CSRF
  ├─ Prompt / Analysis / Batch / Schedule
  ├─ Provider Actual Token Usage
  └─ Recommendation Run / Item / Feed / Interaction
  ↓
MySQL
  ↑
Information Hub Web
```

关键语义：

- Collector 与主系统通过 HTTP 解耦，不直接访问 MySQL；
- 来源数据经过安全清理后保留，标准化字段不能取代原始数据；
- 内容变化会产生不可变 Snapshot，AI Analysis 必须绑定具体 Snapshot；
- Prompt Version、Definition Version 和 Provider Invocation 均可追溯；
- Actual Token 只采信 Provider Usage，预估 Token 只用于 Preview 和预算；
- Recommendation 使用已完成的相关性 Analysis 做确定性计算，不调用 AI Provider；
- Manual 与 Schedule 共用 Analysis Batch Engine。

## 当前能力

### 采集与归档

- 使用真实 Chrome 和 CDP 采集 BOSS 职位列表、明文薪资和职位详情；
- 采集结果增量写入本地 JSON/CSV；
- 完成列表和详情落盘后，每个职位转换为一个 `InformationEnvelope` 并逐条提交 Hub；
- 网络或 Hub 临时失败时写入本地 Outbox，支持独立补传；
- Hub 通过来源身份实现幂等写入，并根据内容 Hash 维护当前数据与历史 Snapshot。

### Web 浏览

- 职位列表、搜索、筛选、排序和分页；
- 职位详情和历史 Snapshot；
- Session 保护的同源访问；
- 前端不展示 `rawPayload`，也不持有 Collector Token。

### AI Processing

- 用户级提示词方案和不可变版本；
- 平台控制的 System Prompt、Output Schema 与严格输出校验；
- 单条 Analysis、最近 N 天 Preview、确认 Token；
- Manual Batch、Daily Schedule、预算保护和数据库 Worker；
- OpenAI-compatible Provider；
- Provider 实际输入、输出及总 Token 用量统计。

当前 Definition：

```text
informationType = JOB
analysisPurpose = USER_RELEVANCE
analysisDefinitionKey = JOB_USER_RELEVANCE
currentVersion = 2
maxOutputTokens = 5000
```

V1 继续保留，用于历史 Analysis 和冻结 Batch。

### 个性化推荐

- JOB 推荐画像与偏好；
- 基于已完成 Analysis 的候选解析；
- 确定性评分、排序、去重、多样性和推荐理由；
- Recommendation Run、Worker、手动刷新及 Batch 完成后自动触发；
- 推荐 Feed、Viewed、Feedback 和职位联系状态；
- `NOT_INTERESTED`、`CONTACTED_NOT_SUITABLE` 硬排除；
- `CONTACTED` 不排除，不自动读取 BOSS 聊天记录。

## 仓库结构

```text
information-platform/
├─ collectors/boss-zhipin-scraper/  # Python BOSS Collector 与 Hub 适配
├─ backend/information-hub/          # Java 21 / Spring Boot 模块化单体
├─ frontend/information-hub-web/     # Vue 3 / TypeScript / Element Plus
├─ deploy/                           # MySQL Compose 与 Nginx 模板
├─ docs/                             # 架构、数据库、ADR、Contract、TASK
├─ AGENTS.md                         # 仓库级开发约束
└─ PHASE5_PACKAGE_MANIFEST.md        # 当前阶段正式规划入口
```

进入模块开发前，请阅读根目录和对应模块的 `AGENTS.md`。

## 技术栈

| 层 | 技术 |
|---|---|
| Collector | Python 3.10+、Chrome、CDP、Requests、WebSocket Client |
| Backend | Java 21、Spring Boot 3.5、Spring Security、MyBatis-Plus |
| Database | MySQL 8.4、Flyway、JSON |
| Frontend | Vue 3、TypeScript、Vite、Pinia、Element Plus |
| Test | JUnit、Vitest、Playwright、Python unittest |
| Deployment | Docker Compose、Nginx、SSH Tunnel |
| AI | OpenAI-compatible Chat Completions、Spring `RestClient`、Jackson |

精确依赖版本以各模块清单和锁文件为准。

## 本地开发快速开始

### 1. 建立共享 MySQL 的 SSH 隧道

```powershell
ssh -N -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes -L 13306:127.0.0.1:3306 information-platform-server
```

验证：

```powershell
Test-NetConnection 127.0.0.1 -Port 13306
```

共享 MySQL 不直接开放公网。密钥、账号和部署步骤见 [部署说明](deploy/README.md)。

### 2. 启动 Information Hub

首次运行先创建不会提交 Git 的本地配置：

```powershell
Copy-Item backend/information-hub/config/application.yml.example backend/information-hub/config/application.yml
Set-Location backend/information-hub
.\mvnw.cmd spring-boot:run
```

至少配置数据库密码和 Collector Token；需要登录、AI 或推荐时，再配置 Bootstrap、Preview Secret、Provider 和 Worker。默认地址为 `http://127.0.0.1:8080`。

完整说明见 [后端配置](backend/information-hub/CONFIGURATION.md)。

### 3. 启动 Web

```powershell
Set-Location frontend/information-hub-web
npm.cmd ci
npm.cmd run dev
```

访问 `http://localhost:5173/login`。Vite 默认通过同源 `/api` 代理到本机 8080 端口；秘密不得放入任何 `VITE_` 变量。

详见 [前端说明](frontend/information-hub-web/README.md)。

### 4. 运行 BOSS Collector

```powershell
Set-Location collectors/boss-zhipin-scraper
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python scripts/boss_cdp_raw.py --setup-chrome
python scripts/boss_cdp_raw.py --check
python scripts/boss_cdp_raw.py --keyword "Java 后端" --city 上海 --pages 1
```

Hub 提交默认关闭。断网或 Hub 临时失败后的补传命令：

```powershell
python scripts/boss_cdp_raw.py --flush-outbox
```

详见 [Collector 使用说明](collectors/boss-zhipin-scraper/README.md)。

## 重要运行开关

| 能力 | 环境变量 | 默认值 | 关闭时表现 |
|---|---|---|---|
| 真实 AI Provider | `INFORMATION_HUB_AI_ENABLED` | `false` | 不调用外部 Provider |
| Analysis Batch Worker | `INFORMATION_HUB_AI_BATCH_WORKER_ENABLED` | `false` | Batch 停留在 `PENDING` |
| Schedule Dispatcher | `INFORMATION_HUB_AI_SCHEDULE_DISPATCHER_ENABLED` | `true` | 不扫描到期 Schedule |
| Recommendation Worker | `INFORMATION_HUB_RECOMMENDATION_WORKER_ENABLED` | `false` | 刷新只创建 `PENDING` Run |
| Collector Hub 提交 | Collector `information_hub.enabled` | `false` | 只保存本地采集结果 |

每条新 Schedule 本身默认关闭。Recommendation Worker 不调用 AI，但会写入 Run 和 Item。

## 测试

Collector：

```powershell
Set-Location collectors/boss-zhipin-scraper
$env:PYTHONUTF8 = "1"
.\.venv\Scripts\python.exe -m unittest discover -s tests -p "test_*.py"
```

Backend：

```powershell
Set-Location backend/information-hub
.\mvnw.cmd test
```

Frontend：

```powershell
Set-Location frontend/information-hub-web
npm.cmd run typecheck
npm.cmd run test
npm.cmd run test:e2e
npm.cmd run build
```

需要 MySQL 的自动化测试只能使用名称包含 `test` 或 `e2e` 且不包含 `dev` 的隔离数据库。

## 安全边界

- Collector Token、Provider API Key、数据库密码、Bootstrap 密码、Preview Secret、Session Cookie 和 BOSS 登录信息不得提交 Git；
- Collector 不得连接主数据库，浏览器不得持有服务端秘密；
- Provider 原始响应不得未经清理进入业务日志或前端；
- 真实 Provider 和具有副作用的 Worker 必须显式开启；
- MySQL 不得直接开放公网；
- 共享环境禁止 `flyway clean`、`docker compose down -v` 和删除命名数据卷；
- 旧 Flyway Migration 不修改，只能新增 Migration；
- 原始 BOSS 响应只用于本地诊断，不上传 Hub、不提交 Git。

## 当前明确不做

- 微服务、Kafka、RabbitMQ、Redis；
- Elasticsearch、MongoDB、Vector DB；
- RAG、Embedding、Agent；
- Kubernetes、Notification；
- 公共注册、复杂 RBAC、多租户；
- 自动读取 BOSS 聊天记录；
- 非 JOB 类型的真实 AI Definition 和 Recommendation Extension。

## 文档导航

项目与设计：

- [文档索引](docs/README.md)
- [项目背景](docs/PROJECT_CONTEXT.md)
- [总体架构](docs/ARCHITECTURE.md)
- [数据库设计](docs/DATABASE_DESIGN.md)
- [路线图](docs/ROADMAP.md)
- [当前开发状态](docs/CURRENT_STATUS.md)
- [后端日志约定](docs/LOGGING_CONVENTIONS.md)

当前 Phase 5：

- [正式规划包](PHASE5_PACKAGE_MANIFEST.md)
- [Scope](docs/PHASE5_SCOPE.md)
- [Task Model](docs/PHASE5_TASK_MODEL.md)
- [Task Index](docs/PHASE5_TASK_INDEX.md)
- [Task Template](docs/PHASE5_TASK_TEMPLATE.md)
- [Codex Workflow](docs/CODEX_PHASE5_WORKFLOW.md)
- [Codex Interaction Guide](docs/CODEX_PHASE5_INTERACTION_GUIDE.md)

模块入口：

- [BOSS Collector](collectors/boss-zhipin-scraper/README.md)
- [Collector 与 Hub 集成](collectors/boss-zhipin-scraper/INTEGRATION.md)
- [Information Hub 配置](backend/information-hub/CONFIGURATION.md)
- [Information Hub Web](frontend/information-hub-web/README.md)
- [部署与 MySQL 运维](deploy/README.md)
- [Web 部署](deploy/WEB_DEPLOYMENT.md)

历史阶段：

- [Phase 3 完成总结](docs/PHASE3_COMPLETION.md)
- [Phase 4 正式规划包](PHASE4_PACKAGE_MANIFEST.md)
- [Phase 4 完成总结](docs/PHASE4_COMPLETION.md)
- [Phase 4 设计摘要](PHASE4_REVIEW_SUMMARY.md)

## 合规说明

BOSS Collector 仅用于个人求职、学习、技术研究和处理用户本人有权访问的数据。使用者应遵守目标网站协议、访问规则、适用法律和合理访问频率，不得用于批量滥用、绕过安全校验或对目标网站造成负担。
