# TASK-048：Prepare v0.1.0-beta.1 Baseline

状态：VERIFYING

所属阶段：Phase 5

类型：Integration

优先级：P1

创建日期：2026-08-14

## 1. Problem

`dev` 最新提交作为仓库首个正式 Baseline `v0.1.0-beta.1` 候选执行 Release Validation 时，发现以下发布阻塞：

1. 后端完整测试中，`IdentitySecurityIntegrationTest` 仍断言 TASK-046 实施前的英文认证失败消息，与当前正式中文公共 API 行为不一致；
2. `V3MigrationImmutabilityTest` 直接对工作区字节计算 Hash，在 Windows checkout 将 Git LF 转换为 CRLF 后误报历史 Migration 被修改；
3. README、docs README、ROADMAP 与根 AGENTS 仍保留 TASK-045 尚未创建或 Next Task 为 TASK-047 等过期事实；
4. 仓库准备发布 `v0.1.0-beta.1`，但后端仍为 `0.0.1-SNAPSHOT`，前端仍为 `0.1.0`，发布产物元数据未对齐。

这些问题不会新增产品功能，但会导致标准后端测试失败、发布文档失真或 Baseline Artifact 版本不准确，因此当前候选不能通过 Release Validation。

## 2. Expected Behavior

- `IdentitySecurityIntegrationTest` 同时验证稳定错误码 `AUTHENTICATION_FAILED` 和正式中文消息“用户名或密码错误”，不修改生产代码已冻结的中文行为；
- V3 Migration 不可变校验在 Windows CRLF 和 Linux LF checkout 下结果一致，真实 SQL 内容变化仍必须失败；
- 历史 V3 Migration 文件本身保持不变，测试不得删除或禁用；
- README、docs README、ROADMAP、根 AGENTS、CURRENT_STATUS 与 PHASE5_TASK_INDEX 对 Phase 5 的任务事实保持一致；
- 仓库 / GitHub Release 版本为 `v0.1.0-beta.1`；Backend 和 Frontend 组件版本均为 `0.1.0-beta.1`；
- Collector 保持独立组件版本 `2.1.0`；Flyway Schema Version 保持 `V4`；
- 本地可执行的完整自动化测试和构建通过，TASK 最终进入 `VERIFYING`，等待 Baseline Release Validation 或用户验收，不直接标记 `DONE`。

## 3. Scope

### Included

- 修复 `IdentitySecurityIntegrationTest` 三处过期英文消息断言，并保留稳定错误码验证；
- 将 `V3MigrationImmutabilityTest` 改为跨平台稳定的规范化换行后 SHA-256 校验；
- 修改 Backend Maven Artifact Version 为 `0.1.0-beta.1`；
- 修改 Frontend package version 为 `0.1.0-beta.1`，同步 `package-lock.json`；
- 同步 Phase 3 / Phase 4 Full-stack E2E 启动器使用 `0.1.0-beta.1` 后端 JAR；
- 明确仓库 Baseline 版本与 Collector 独立版本策略；
- 同步 README、docs README、ROADMAP、根 AGENTS、CURRENT_STATUS 和 PHASE5_TASK_INDEX；
- 执行 Backend、Frontend、Collector 完整自动化检查，以及当前环境可运行的 Phase 4 Full-stack E2E；
- 记录 Phase 3 Full-stack E2E 的真实环境可执行情况。

### Not Included

- 不新增或调整任何产品功能、业务流程、API 行为或安全边界；
- 不修改生产认证错误消息、错误码或 HTTP 状态码；
- 不修改 V1～V4 任何历史 Flyway Migration，也不增加新 Migration；
- 不改变数据库 Schema Version `V4`；
- 不修改 Collector 独立组件版本 `2.1.0`；
- 不引入新的 CI、基础设施、依赖或发布自动化；
- 不 merge `main`，不创建 Git Tag 或 GitHub Release，不 commit / push。

## 4. Preconditions / Existing Constraints

- 遵守根 AGENTS.md、Backend / Frontend / Documentation 模块 AGENTS.md、Phase 5 Scope、Rolling Task Model 和 Codex Workflow；
- TASK-046 与 `web-ui-behavior-v1` 已冻结浏览器公共 API 使用稳定 `code` 和安全中文 `message` 的行为；
- 内部异常诊断文本可以保留英文，不能为了旧测试改变正式公共响应；
- 已发布 Migration 不得修改；不可变测试必须能检测真实 SQL 内容变化；
- 版本元数据变化不得影响数据库版本、API Contract、认证授权、Owner 隔离或运行时业务语义；
- 测试结果必须来自实际执行，环境限制不得伪造为 PASS。

## 5. Investigation Notes

- Phase 5 创建本 TASK 前没有其它 Active / VERIFYING / DEFERRED TASK，Next Task Number 为 `TASK-048`；本问题边界独立且具有完整发布验收标准，因此创建 TASK-048。
- `GlobalApiExceptionHandler` 对认证异常返回 `code=AUTHENTICATION_FAILED`、`message=用户名或密码错误`；`BrowserApiErrorMessages`、前端错误映射、TASK-046 实施记录及 `web-ui-behavior-v1` 均与该行为一致。
- `IdentitySecurityIntegrationTest` 已验证稳定错误码，但第 111、126、143 行附近仍断言旧英文消息，是测试同步遗漏。
- V3 Migration 的 Git 索引内容为 LF，Windows 工作区为 CRLF；原测试读取原始字节，因此相同 Git 内容产生不同 SHA-256。
- 对 UTF-8 文本先将 CRLF 和 CR 规范化为 LF，再计算既有 Accepted SHA-256，可以只忽略平台换行差异，同时继续检测所有非换行 SQL 内容变化。
- 当前 Flyway Migration 为 V1～V4，真实 MySQL 校验的 Schema Version 为 `4`，本 TASK 无数据库结构变化。
- Backend 实施前版本为 `0.0.1-SNAPSHOT`，Frontend 实施前版本为 `0.1.0`，Collector `pyproject.toml` 独立版本为 `2.1.0`。
- 首次执行 Phase 4 Full-stack E2E 时发现 Phase 3 / Phase 4 启动器仍硬编码旧 `0.0.1-SNAPSHOT` JAR；该问题会导致 E2E 验证遗留产物，因此属于版本切换的必要配套修正。

## 6. Implementation Decision

- 认证测试只更新公开消息期望值，继续逐项断言 `AUTHENTICATION_FAILED`，不修改生产代码。
- V3 不可变测试使用 UTF-8 解码，并把 `\r\n` / `\r` 规范化为 `\n` 后计算 SHA-256；不采用修改历史 SQL、删除测试或仅依赖 checkout 配置的方案。
- 仓库 Baseline 使用 Git / GitHub Release 版本 `v0.1.0-beta.1`；Backend / Frontend Artifact 使用无 `v` 前缀的 `0.1.0-beta.1`。
- Collector 保持独立组件版本 `2.1.0`，在 TASK 与发布事实文档中明确，不强制与仓库 Baseline 同版本。
- Phase 3 / Phase 4 Full-stack E2E 启动器同步引用 `information-hub-0.1.0-beta.1.jar`，确保验证实际 Baseline Artifact。
- 不更新 Architecture、Database Design、API Contract 或 ADR，因为产品行为、架构、数据库和接口结构均无变化。

## 7. Acceptance Criteria

- [x] 原 Baseline Validation 的四类 blocker 均已解决。
- [x] 三种认证失败场景同时断言 `AUTHENTICATION_FAILED` 和“用户名或密码错误”。
- [x] V3 不可变测试在 CRLF/LF checkout 下稳定，并仍能检测真实 SQL 内容变化。
- [x] V3 Migration 文件未修改，Flyway 仍为 V4。
- [x] Backend Artifact Version 为 `0.1.0-beta.1`，完整测试和跳过测试构建通过。
- [x] Frontend Version 与 lockfile 均为 `0.1.0-beta.1`，typecheck、unit tests 和 build 通过。
- [x] Collector Version 仍为 `2.1.0`，完整自动化测试通过。
- [x] README、docs README、ROADMAP、根 AGENTS、CURRENT_STATUS 与 Task Index 的 Phase 5 事实一致。
- [x] 当前没有未说明的 Active / DEFERRED TASK，TASK-048 进入 `VERIFYING`，Next Task Number 为 `TASK-049`。
- [x] 当前环境可执行的 Phase 4 Full-stack E2E 通过；Phase 3 E2E 如受专用环境限制则如实记录。
- [x] `git diff --check` 与 `git diff --cached --check` 通过。

## 8. Adjustment Log

- Phase 4 Full-stack E2E 首次运行虽然通过，但日志显示它启动了遗留的 `information-hub-0.0.1-SNAPSHOT.jar`。已将 Phase 3 / Phase 4 E2E 启动器同步到 `information-hub-0.1.0-beta.1.jar`，并重新运行 Phase 4 E2E；新日志确认实际启动 `InformationHubApplication v0.1.0-beta.1`，测试通过。
- 该调整是 Backend Artifact 版本切换的必要配套，不新增产品功能，无需拆分后续 TASK。
- Baseline Candidate 的 GitHub `Backend tests` Job 原先没有 MySQL Service，也没有注入 `INFORMATION_HUB_DB_*` / `INFORMATION_HUB_TEST_DB_*`。普通 Spring 上下文会保留开发库默认地址，真实数据库集成测试则因缺少测试库环境变量而不能作为可靠门禁；现已复用 Phase 3 E2E 验证过的 `mysql:8.4` Service 模式，为该 Job 增加 Runner 生命周期内的独立 `information_hub_backend_test` 临时库和仅限 CI 的固定凭据。
- Maven 测试同时通过 `INFORMATION_HUB_DB_*` 与 `INFORMATION_HUB_TEST_DB_*` 连接上述临时测试库，并显式启用 Flyway，使空库按当前 V1～V4 正常初始化。该 Job 不读取个人 MySQL、SSH Tunnel、云数据库或数据库 Secret；Runner 销毁时 Service 与数据一并销毁，且未修改业务 `application.yml` 默认开发配置。
- Repository checks 中 `git grep` 的退出码 `1` 表示“没有匹配项”，但新版 PowerShell 的原生命令错误转换可能在脚本读取 `$LASTEXITCODE` 前按 `$ErrorActionPreference = 'Stop'` 终止 Job。现仅在该扫描调用期间关闭转换并保存退出码：`0` 仍检查并阻止私钥内容，`1` 视为正常，其他非零值仍作为扫描失败。
- 上述两项均为本 TASK 的 Baseline CI Adjustment，不改变业务功能、数据库 Schema、API Contract 或安全边界，无需创建新 TASK；TASK-048 保持 `VERIFYING`。

## 9. Test / Verification Record

- `java -version`：通过，Java `21.0.11`。
- `backend/information-hub/mvnw.cmd "-Dtest=IdentitySecurityIntegrationTest,V3MigrationImmutabilityTest" test`：通过，9 tests，0 failures，0 errors，0 skipped。
- `backend/information-hub/mvnw.cmd test`：通过，276 tests，0 failures，0 errors，3 skipped；Maven `BUILD SUCCESS`。
- `backend/information-hub/mvnw.cmd -DskipTests package`：通过，生成 `target/information-hub-0.1.0-beta.1.jar`。
- 后端真实 MySQL / Flyway 校验：成功验证 4 个 Migration，当前 Schema Version 为 `4`，无需迁移。
- `frontend/information-hub-web/npm.cmd run typecheck`：通过。
- `frontend/information-hub-web/npm.cmd run test`：通过，32 test files、95 tests。
- `frontend/information-hub-web/npm.cmd run build`：通过，`vue-tsc` 与 Vite build 成功，构建版本为 `0.1.0-beta.1`。
- `collectors/boss-zhipin-scraper/python -m unittest discover -s tests -p "test_*.py"`：通过，144 tests。
- `frontend/information-hub-web/npm.cmd run test:e2e:phase4`：通过，Chromium 1/1；重新运行时确认启动的是 `information-hub-0.1.0-beta.1.jar`，完整推荐生命周期证据已输出。
- `frontend/information-hub-web/npm.cmd run test:e2e:phase3`：未进入测试执行；预检因缺少 `INFORMATION_HUB_E2E_DB_URL` 退出，未标记为 PASS，保留给 GitHub CI 最终发布门禁。
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\\scripts\\verify-project.ps1`：通过；当前仓库私钥标记扫描无匹配，实际覆盖 `git grep` 合法退出码 `1`，脚本正常返回 `0`。
- 使用项目现有 Frontend `yaml` 依赖解析 `.github/workflows/ci.yml`，并断言 Backend Service 镜像、测试库名称及 Maven 的 7 个数据库 / Flyway 环境变量：通过。
- 本地按 CI 环境变量映射执行 `backend/information-hub/mvnw.cmd test`：未通过，268 tests、0 failures、39 errors、2 skipped；首个真实错误为当前本机 `127.0.0.1:13306/information_hub_test` 无 MySQL 监听导致 `Communications link failure`，后续 38 项为 Spring Context 级联错误。该结果未标记为 PASS，也未发现 SQL、Migration 或业务断言失败；GitHub Job 将使用自身健康检查通过的 MySQL 8.4 Service 完成最终验证。
- 当前机器未另外启动或修改个人 MySQL；CI 自包含数据库的最终运行结果需在本次修改 push 后由 GitHub Actions 验证。
- 清除当前 Maven 进程的主库、测试库和 Flyway 环境变量后再次执行 `backend/information-hub/mvnw.cmd test`：未通过，276 tests、0 failures、15 errors、34 skipped；首个错误仍为默认本地 MySQL 未监听导致 Flyway `Communications link failure`，其余为 Context 级联错误，说明本地现有测试仍按原配置工作但当前缺少其数据库前置条件。
- 本机 Docker Desktop Linux Engine 未运行，无法在本地启动等价 MySQL Service；未将该环境限制伪报为 CI PASS。
- `git diff --check`：通过。
- `git diff --cached --check`：通过。

## 10. Documentation Sync

- [x] `docs/CURRENT_STATUS.md`
- [x] `docs/PHASE5_TASK_INDEX.md`
- [x] `docs/ARCHITECTURE.md`（无架构事实变化，无需修改）
- [x] `docs/DATABASE_DESIGN.md`（无数据库事实变化，无需修改）
- [x] Contract（正式 API 行为不变，无需修改）
- [x] ADR（无新架构决策，无需修改）
- [x] AGENTS
- [x] README / ROADMAP
- [x] Release Version Strategy

## 11. Completion Checklist

- [x] Scope review
- [x] Security / Owner review（无边界变化）
- [x] Logging review（无生产业务代码变化）
- [x] Database / migration review
- [x] Contract compatibility review
- [x] Tests
- [x] `git diff --check`
- [x] Implementation Record
- [x] Task Index
- [x] Current Status
- [x] 当前 TASK 相关文件已 `git add`
- [x] 未自动 commit / push / merge / tag / release

## 12. Implementation Record

- 依据 TASK-046 实施记录与 `web-ui-behavior-v1` Contract，保留生产端正式中文认证响应，只将 `IdentitySecurityIntegrationTest` 三处旧英文消息断言更新为“用户名或密码错误”；三处均继续断言稳定错误码 `AUTHENTICATION_FAILED`。
- `V3MigrationImmutabilityTest` 改为按 UTF-8 读取 SQL，把 CRLF 和孤立 CR 统一为 LF 后再计算原 Accepted SHA-256。该方案仅消除 checkout 换行差异，任何非换行 SQL 内容变化仍会改变 Hash；V3 Migration 文件本身未修改。
- Backend Maven 版本由 `0.0.1-SNAPSHOT` 更新为 `0.1.0-beta.1`；Frontend package 与 lockfile 由 `0.1.0` 更新为 `0.1.0-beta.1`；Phase 3 / Phase 4 E2E 启动器同步到新 JAR 名称。
- Collector 未修改并保持独立版本 `2.1.0`；Flyway 仍为 V4，未新增或修改 Migration。
- README、docs README、ROADMAP、根 AGENTS、CURRENT_STATUS 与 Task Index 已同步 TASK-045～TASK-048、`VERIFYING`、Next Task `TASK-049` 和版本策略事实。
- `.github/workflows/ci.yml` 的 Backend tests Job 新增 `mysql:8.4` Service、健康检查、独立测试库与 CI-only 账号，并只在 Maven test Step 注入主 DataSource 和数据库集成测试所需环境变量；本地默认配置、生产部署配置和 Secret 边界均未改变。
- `scripts/verify-project.ps1` 对 `git grep` 使用局部的原生命令退出码处理，明确区分匹配、无匹配和扫描异常，消除合法退出码 `1` 的 Repository checks 误报，同时保留敏感内容阻断能力。
- 未修改生产业务功能、数据库、API Contract、Architecture、ADR 或安全边界；未执行 commit、push、merge、tag 或 GitHub Release。

## 13. Commit

建议提交信息：

```text
完成 TASK-048：准备 v0.1.0-beta.1 Baseline
```
