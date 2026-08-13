# TASK-047：Analysis 批次限制配置化与预览结果文案澄清

状态：DONE

所属阶段：Phase 5

类型：Integration

优先级：P2

创建日期：2026-08-13

## 1. Problem

“手动分析”页面允许用户输入候选时间范围、最大候选数量和预估 Token 预算，但这些参数的默认值与平台最大值目前分别写死在前后端代码中：

- 手动分析页面固定使用 `3 / 20 / 75000` 作为默认值，使用 `14 / 50 / 200000` 作为最大值；
- 定时分析页面重复写入相同数值；
- 后端 `AnalysisPreviewLimits` 使用静态常量执行相同的默认值和硬上限校验；
- 外部 `application.yml`、配置模板和环境变量均没有调整入口；
- 前端没有从后端读取当前有效限制，单独修改任一端会造成前后端不一致。

部署管理员因此无法通过服务端配置调整受控批次上限，只能修改代码并重新构建前后端。

Preview 结果中的“已选择”直接展示 `selectedCount`，其真实含义是“经过候选数量上限和预估 Token 预算后，本批预计执行的候选数量”，并非用户手动勾选的条目。当前用语会误导用户寻找不存在的选择控件。

## 2. Expected Behavior

- 部署管理员可以通过 Information Hub 服务端配置调整以下默认值和最大值：
  - 候选时间范围；
  - 最大候选数量；
  - 预估 Token 预算。
- 未提供新配置时保持现有行为：默认值为 `3 / 20 / 75000`，最大值为 `14 / 50 / 200000`。
- 配置支持外部 `application.yml` 和对应环境变量，修改后重启 Information Hub 生效；不要求热更新。
- 后端是限制的唯一事实来源，Manual 与 Schedule 继续共用同一组 Preview / Batch 限制。
- 应用启动时验证所有值为正数、默认值不超过最大值，且数值处于 Java 类型、数据库字段和 Token 聚合的安全范围；无效配置应明确启动失败。
- 浏览器通过 Session 保护的只读 API 获取当前默认值、最小值和最大值；响应不得暴露 HMAC Secret、Provider 配置或其它秘密。
- 手动分析和定时分析页面使用后端返回的默认值初始化新表单，并使用后端返回的最大值限制输入，不再独立写死平台上限。
- 服务端校验仍是最终边界，绕过前端提交非法或超限值时返回稳定错误码和准确中文消息。
- Preview Token 继续冻结本次实际限制、候选顺序、Estimate 和指纹；Confirm 的 Owner、到期、篡改和漂移校验保持不变。
- 已创建 Batch 的历史限制和结果保持不可变，配置变化不得回写历史 Batch。
- 配置上限降低时不得静默修改已有 Schedule：
  - 已有超限 Schedule 保持可查询；
  - 页面明确标识其配置超过当前平台上限，并要求用户修改；
  - 超限 Schedule 不得成功 Preview、重新启用或产生新的可执行 Batch / Provider 调用；
  - 调整到当前范围并保存后恢复正常。
- Preview 指标“已选择”改为“本批预计分析条数”；API 字段 `selectedCount` 保持不变。

## 3. Scope

### Included

- 在现有 `information-hub.ai.preview` 配置域增加 Preview / Batch 默认值、最大值及环境变量映射。
- 增加启动期配置校验和安全诊断。
- 将 `AnalysisPreviewLimits` 改为后端配置驱动的统一限制策略。
- 保持 Manual 与 Schedule 共用同一限制解析、Candidate Resolver 和 Budget Guard。
- 新增 Session 保护的只读 Analysis 限制 API。
- 手动分析页面动态加载限制并据此初始化、约束表单。
- 定时分析页面动态加载同一限制；已有超限 Schedule 保持可读并显示不兼容状态，禁止预览、启用或按超限配置保存。
- Dispatcher 对超限既有 Schedule 安全跳过，不创建可执行 Batch、不调用 Provider。
- 将 Preview 指标文案改为“本批预计分析条数”。
- 更新后端配置、API、Preview、Schedule、Dispatcher 测试。
- 更新前端 API、手动分析、定时分析测试及必要 Browser E2E。
- 同步 `analysis-batch-v1`、`analysis-schedule-v1` 和后端配置文档。

### Not Included

- 不提供浏览器管理员页面修改平台限制。
- 不把限制保存到数据库，不支持热更新或配置中心。
- 不增加用户级、Prompt Profile 级平台上限。
- 不改变 Token Estimate 算法、Definition `maxOutputTokens`、Provider 参数或 Actual Token 语义。
- 不改变候选资格、稳定排序、已分析过滤和连续预算前缀规则。
- 不允许普通用户绕过服务端最大值。
- 不修改历史 Batch、Analysis、Invocation 或 Usage。
- 不修改数据库结构、Flyway Migration、Session、CSRF、Owner 隔离或 Recommendation 生命周期。
- 不引入通用动态配置平台或顺带重构其它 AI 页面。

## 4. Preconditions / Existing Constraints

- 遵守根 `AGENTS.md`、前后端模块 `AGENTS.md`、`docs/LOGGING_CONVENTIONS.md` 和现有 Accepted ADR / Contract。
- Manual 与 Schedule 必须继续共用 Candidate Resolver、Preview、Budget Guard、Batch Engine 和 Usage 聚合。
- Preview 不调用 Provider、不持久化；Confirm 继续使用 10 分钟 HMAC Token，不信任客户端回传候选或 Estimate。
- Analysis 必须绑定不可变 Snapshot；配置变化不得改变历史 Analysis、Batch 或 Invocation 语义。
- Actual Token 只能来自 Provider Usage；本 TASK 的预算仍是 Estimated Token Budget，不是实际费用保证。
- 服务端配置属于部署权限边界；浏览器只读取可公开数值，不能读取或修改服务端秘密。
- 后端必须校验所有请求，前端输入限制不能代替服务端校验。
- 旧 Flyway Migration 不得修改。

## 5. Investigation Notes

- Phase 5 当前没有 Active TASK，Next Task Number 为 `TASK-047`。本问题具有独立 Problem、Expected Behavior、跨前后端 Scope 和验收标准，不属于已完成 TASK-045/046 的 Adjustment。
- `analysis-batch-v1.md` 当前冻结 `3/14`、`20/50`、`75000/200000` 默认值与 hard max。
- `analysis-schedule-v1.md` 使用同一组限制；Schedule 保存三个请求值，触发时复用 Preview 限制解析。
- 后端 `AnalysisPreviewLimits` 同时被 Manual Preview、Schedule 创建/更新、Schedule Preview 和 Dispatcher 使用，是共享限制入口。
- `AnalysisPreviewProperties` 已绑定 `information-hub.ai.preview`，目前只包含 HMAC Secret，可在现有配置域内扩展。
- `ManualAnalysisView.vue` 和 `SchedulesView.vue` 分别写死相同默认值和输入最大值，当前没有限制查询 API。
- `selectedCount` 是 Candidate Limit 与 Token Budget 共同作用后的预计可执行数量；后端按稳定顺序选择预算允许的连续前缀，不支持逐条勾选。
- Preview Token 已包含三个限制及候选/Estimate 指纹。配置变化后旧 Token 如不再满足当前限制，应沿用安全冲突拒绝语义，不应绕过新上限。
- Schedule 和 Batch 对应字段使用 `INT UNSIGNED` 与 `BIGINT UNSIGNED`，当前无需 Migration。
- 配置下调可能使已有 Schedule 超限。现有 Dispatcher 已将限制解析异常归类为 `INVALID_CONFIGURATION` 并推进计划点、不调用 Provider；本 TASK 需补齐前端可见性和编辑恢复行为。
- 提高上限会增加 Batch Item、数据库写入、Worker 运行时间和 Provider 费用，因此配置只允许部署管理员控制，默认值保持不变。
- 规划创建前 Git 工作区干净，当前无未提交业务修改。

## 6. Implementation Decision

- 采用用户确认的方案 C：后端配置作为唯一限制来源，Web 通过只读 API 动态获取，Manual 与 Schedule 保持一致。
- 在 `information-hub.ai.preview` 下扩展六个非敏感参数，并提供对应环境变量。
- 后端提供认证用户可读的限制元数据接口，只返回三个字段组的 `defaultValue / minimum / maximum`。
- 保留 Preview/Confirm 请求与响应字段；`selectedCount` 不改名。新增只读接口和 UI 文案变化按向后兼容 Contract 演进处理。
- 既有超限 Schedule 不自动改写、不继续按超限值执行；页面提示用户显式修改，Dispatcher 安全跳过且不调用 Provider。
- 不需要数据库 Migration 或新 ADR；需要更新 Analysis Batch / Schedule Contract 和后端配置文档。模块边界不变，预计无需更新 Architecture。

## 7. Acceptance Criteria

- [x] 未配置新增参数时仍使用默认 `3 / 20 / 75000` 与最大 `14 / 50 / 200000`。
- [x] 外部 YAML 和对应环境变量可以调整三个默认值与三个最大值，重启后生效。
- [x] 后端是唯一限制来源，手动与定时分析页面不再写死平台最大值。
- [x] 限制 API 要求登录，只返回非敏感数值。
- [x] 手动分析页面按后端默认值初始化，并允许输入到配置最大值。
- [x] 定时分析新建表单使用同一默认值和最大值，保存、Preview 和触发共用同一策略。
- [x] 绕过前端提交零值、负值或超限值时，后端返回稳定 `400` 错误码和准确中文消息。
- [x] 默认值超过最大值、非正数或超出安全范围时，Information Hub 明确启动失败且日志不泄密。
- [x] 提高上限后，可以创建超过旧 `50 / 200000`、但不超过新上限的 Preview 和 Schedule。
- [x] 降低上限不修改历史 Batch，也不静默修改已有 Schedule。
- [x] 已有超限 Schedule 仍可查询，页面明确提示，并且不能 Preview、重新启用或产生新的可执行 Batch / Provider 调用。
- [x] 已有超限 Schedule 调整到当前范围并保存后恢复正常。
- [x] Preview Token 继续冻结限制和候选指纹；过期、篡改、Owner 不符、候选漂移或配置不兼容均安全拒绝且不创建 Batch。
- [x] Preview 指标显示“本批预计分析条数”，不再显示“已选择”。
- [x] `selectedCount` API 字段、Estimate 算法、连续预算前缀、历史 Usage 和 Actual Token 语义不变。
- [x] 后端配置/限制/API/Preview/Schedule/Dispatcher targeted tests 通过。
- [x] 前端 API、手动分析、定时分析自动化测试通过；相关 Browser E2E 脚本已同步，人工浏览器验收通过。
- [x] `analysis-batch-v1`、`analysis-schedule-v1` 和 `CONFIGURATION.md` 已同步。
- [x] 未修改数据库、Flyway、认证授权、Owner 隔离、Analysis Snapshot 或 Recommendation 生命周期。
- [x] `git diff --check` 通过。

## 8. Adjustment Log

当前无 Adjustment。用户已按交付步骤完成人工验证并确认通过，没有遗留调整项，也不需要拆分新的后续 TASK。

## 9. Test / Verification Record

实施阶段自动化记录如下：

```text
环境：
- java -version：Java 21.0.11
- .\mvnw.cmd -version：Apache Maven 3.9.16 / Java 21.0.11

后端聚焦测试：
.\mvnw.cmd "-Dtest=AnalysisPreviewLimitsTest,AnalysisPreviewPropertiesTest,AnalysisPreviewControllerTest,AnalysisPreviewServiceTest,AnalysisScheduleServiceTest,AnalysisScheduleTransactionServiceTest,BrowserApiErrorMessagesTest" test
结果：30 tests，0 failures，0 errors，BUILD SUCCESS
覆盖：默认值/覆盖值/非法启动配置、认证只读 API、中文错误、旧上限以上 Preview/Schedule、旧 Token 配置漂移、超限 Schedule 禁止重新启用和 Dispatcher 安全跳过。

前端聚焦测试：
npm.cmd test -- --run src/api/phase3Api.spec.ts src/views/ManualAnalysisView.spec.ts src/views/SchedulesView.spec.ts
结果：3 files / 7 tests 全部通过

前端完整测试：
npm.cmd test
结果：32 files / 95 tests 全部通过

前端构建：
npm.cmd run build
结果：typecheck 与 Vite production build 通过
```

已更新 Phase 3 Browser E2E，使真实后端以非默认最大值 `30 / 100 / 500000` 启动并校验只读限制 API；当前环境没有 `INFORMATION_HUB_E2E_DB_*` 等专用测试 MySQL 凭据，因此本轮未运行真实 Browser E2E，也未使用开发库替代。

后端完整 `mvnw test` 已执行，但本机 `127.0.0.1:13306` 测试 MySQL 未运行，既有数据库集成测试无法建连；结果为 267 tests、1 failure、39 errors、2 skipped。唯一非连接失败是未修改的 V3 Migration Windows 工作区换行校验基线；`src/main/resources/db/migration` 无 Git 修改。本结果不影响上述 TASK-047 聚焦测试结论。完整后端与自动 Browser E2E 未在缺少专用测试 MySQL 的当前环境中执行通过，该环境限制已如实保留，不以开发库替代。

最终人工验收（2026-08-13）：用户已按交付的人工验证步骤完成 TASK-047 验收并确认通过。Acceptance Criteria 已逐项复核，无未完成的产品行为或 Adjustment，任务状态由 `VERIFYING` 更新为 `DONE`。

## 10. Documentation Sync

- [x] `docs/CURRENT_STATUS.md`
- [x] `docs/PHASE5_TASK_INDEX.md`
- [x] `docs/ARCHITECTURE.md`（模块边界无变化，无需修改）
- [x] `docs/DATABASE_DESIGN.md`（数据库事实无变化，无需修改）
- [x] Contract：`analysis-batch-v1.md`、`analysis-schedule-v1.md`
- [x] `backend/information-hub/CONFIGURATION.md`
- [x] ADR（现有决策未变化，无需新增或修改）
- [x] AGENTS（开发规则无变化，无需修改）
- [x] README / ROADMAP（项目级导航和阶段事实无变化，无需修改）

## 11. Completion Checklist

- [x] Scope review
- [x] Security / Owner review
- [x] Logging review
- [x] Database / migration review
- [x] Contract compatibility review
- [x] Tests（聚焦测试、前端全量测试与构建通过；人工浏览器验收通过；完整后端 / 自动 Browser E2E 的专用 MySQL 环境限制已记录）
- [x] `git diff --check`
- [x] Implementation Record
- [x] Task Index
- [x] Current Status
- [x] 当前 TASK 相关文件已 `git add`
- [x] 未自动 commit / push

## 12. Implementation Record

- 在 `information-hub.ai.preview` 增加六个默认值/最大值配置及环境变量映射，保留 `3 / 20 / 75000` 与 `14 / 50 / 200000` 兼容默认；启动期校验正数、默认值不超过最大值及技术安全边界。
- 新增统一 `AnalysisPreviewLimitPolicy`，Manual Preview、Confirm 重算、Schedule 保存/启用和 Dispatcher 全部复用同一限制来源。
- 新增 Session 保护的 `GET /api/v1/ai/analysis-batches/limits`，只返回三组 `defaultValue / minimum / maximum`，不暴露 HMAC 或 Provider 配置。
- 配置下调后，旧 Preview Token 按 `ANALYSIS_PREVIEW_DRIFTED` 冲突拒绝；已有超限 Schedule 继续可读，但后端禁止重新启用，Dispatcher 以 `INVALID_CONFIGURATION` 跳过且不创建 Batch。
- 手动分析和定时分析页面动态读取服务端默认值与上限；定时页面标记已有超限配置、禁止预览/启用/超限保存，并允许用户调整后恢复。
- Preview 指标文案统一为“本批预计分析条数”，保留 API 字段 `selectedCount`。
- 同步 Analysis Batch / Schedule Contract、后端配置说明、外部配置示例、Phase 3 Browser E2E、Task Index 与 Current Status。
- 未修改数据库、Flyway、Architecture、ADR、认证/CSRF/Owner 隔离、Estimate 算法、Provider 或历史 Batch/Usage 语义。
- 2026-08-13 人工验收确认通过；未产生 Adjustment 或需要拆分的后续问题，实施内容保持不变并完成收尾。

## 13. Commit

建议提交信息：

```text
完成 TASK-047：配置化 Analysis 批次限制并澄清预览结果文案
```
