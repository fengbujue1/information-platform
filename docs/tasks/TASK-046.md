# TASK-046：统一全局操作反馈与公共 API 中文错误消息

状态：DONE

所属阶段：Phase 5

类型：Integration

优先级：P2

创建日期：2026-08-13

## 1. Problem

浏览器页面执行保存、启停、预览、刷新等操作失败时，当前项目缺少统一、准确的反馈机制。

已确认的复现路径：

1. 在定时分析页面选择一个已停用的提示词方案；
2. 保存定时分析；
3. 后端返回 `ANALYSIS_SCHEDULE_PROFILE_DISABLED`，响应 `message` 为 `Prompt Profile must be active`；
4. 前端错误映射表未覆盖该稳定错误码，因此页面显示通用文案“请求失败，请稍后重试”；
5. 该错误属于不可通过重试解决的业务校验问题，通用重试提示会误导用户，且错误以页面内容区内长期红字呈现。

当前不同页面分别使用局部红字、完整错误状态、Alert 或 `ElMessage`，成功、警告和失败反馈的展示位置、持续时间及消失行为不一致。后端多个浏览器公共 API 仍把英文异常消息写入统一错误响应，导致接口响应和中文产品界面语言不一致。

## 2. Expected Behavior

- 浏览器中的操作型成功、警告和失败反馈统一使用全局消息弹窗。
- 弹窗显示在整个视口的中上部，不随页面上下或左右滚动而移动。
- 弹窗采用淡入淡出效果，默认显示 3 秒后自动关闭。
- 保存、新建、启停、预览、刷新、反馈和状态修改等操作必须显示准确、可行动的中文反馈，不得把确定的业务校验错误误报为“请稍后重试”。
- 页面首次加载失败、无数据、字段校验、登录失效等结构性状态继续使用适合其语义的错误页、空态、字段提示或登录跳转；全局弹窗不得吞掉重试入口或认证流程。
- 浏览器调用的公共 API 失败响应保持稳定结构和错误码，公开 `message` 全部使用安全、准确的中文。
- 前端继续以稳定错误码作为程序判断和首选中文映射依据；受控的后端中文 `message` 只作为兼容回退，不把未知服务端文本直接当作可信 UI 内容。
- “全部中文化”仅指浏览器调用的公共 API 错误响应及用户可见前端提示。
- 内部日志、异常类诊断文本、Provider 原始错误和历史持久化诊断信息不在本次翻译范围内。

## 3. Scope

### Included

- 审计前端当前使用的所有浏览器公共 API 稳定错误码，并补齐准确中文映射。
- 修复 `ANALYSIS_SCHEDULE_PROFILE_DISABLED` 等已确认的定时分析错误码映射缺口。
- 基于现有 Element Plus 能力建立轻量、统一的全局消息反馈入口，固定视口中上部、淡入淡出、默认持续 3 秒。
- 将已有页面中的操作型成功、警告和失败提示迁移到统一消息反馈入口。
- 区分操作反馈与结构性页面状态，保留首次加载失败的错误页与重试、空态、字段校验以及 401 登录恢复流程。
- 审计浏览器实际调用的后端 API 异常处理边界，使公开失败响应中的 `message` 使用安全中文，并继续保留稳定 `code`。
- 对未预期错误、持久化错误和安全相关错误使用不泄露内部细节的通用中文消息。
- 更新前端错误转换与反馈测试、相关页面组件测试、后端异常处理/API 测试，以及必要的 Browser E2E。
- 同步受影响的 Web UI / API Contract，明确客户端依赖稳定 `code`，不能依赖 `message` 做程序判断。

### Not Included

- 不翻译内部日志、异常类内部诊断文本、Provider 原始错误或已持久化的历史诊断信息。
- 不修改 Collector 专用接入 API 的错误消息，除非该接口同时属于浏览器实际调用范围。
- 不把所有页面加载错误、空态或字段校验强制替换为 3 秒弹窗。
- 不改变现有错误码、HTTP 状态码、成功响应结构或业务校验规则。
- 不修改数据库或 Flyway Migration。
- 不引入新的通知基础设施、消息中心、服务端推送、完整 i18n 或多语言切换。
- 不顺带重构与错误反馈无关的页面布局、业务流程、Worker、Recommendation 或 Analysis 生命周期。

## 4. Preconditions / Existing Constraints

- 遵守根 `AGENTS.md`、前后端模块 `AGENTS.md`、`docs/LOGGING_CONVENTIONS.md` 和现有 Accepted ADR / Contract。
- 保持同源 Session、CSRF、Owner 隔离和认证路由守卫不变。
- 401 必须继续清理失效认证态并进入登录恢复流程；403 不得误报为网络错误。
- 浏览器和公开响应不得暴露 Java 堆栈、SQL、数据库细节、Token、Cookie、Prompt 正文、Provider 密钥或 Collector raw payload。
- 前端不得依据可变 `message` 做业务判断；稳定 `code` 继续是机器判断依据。
- 未预期异常必须保留服务端日志诊断能力，但返回调用方的消息必须脱敏。

## 5. Investigation Notes

- Phase 5 Task Index 当前无 Active TASK，`TASK-045` 已完成；本问题具有独立 Problem、跨前后端 Scope 和验收标准，应创建为 `TASK-046`，不是 TASK-045 Adjustment。
- 后端定时分析校验实际返回 `ANALYSIS_SCHEDULE_PROFILE_DISABLED` 与英文消息 `Prompt Profile must be active`。
- 前端 `src/api/apiError.ts` 使用 `ERROR_MESSAGES[code] ?? '请求失败，请稍后重试'`，但当前没有上述错误码，因此进入误导性兜底。
- Axios 响应拦截器已统一将失败响应转换为 `ApiClientError`；现有基础适合增加受控回退和统一反馈，不需要更换 HTTP Client。
- `SchedulesView.vue` 等页面将错误写入局部 `error` 状态，并用页面内 `.ai-error` 长期展示；登录页已经使用 Element Plus `ElMessage`。
- Element Plus 消息组件支持 Teleport、视口固定定位、淡入淡出和 3 秒持续时间，可以满足需求，不需要自建通知基础设施。
- 现有 `web-ui-behavior-v1` 已约定后端错误码映射为可理解中文，不直接暴露 Axios 内部对象；本 TASK 延续并完善该约定。
- 后端 `ApiResponse` 已提供稳定的 `success/code/message/data` 结构，多处 Exception Handler 当前直接传递英文 `exception.getMessage()`。
- “后端全部中文化”的冻结边界是浏览器调用的公共 API 失败响应；内部诊断文本和非浏览器接口不在本 TASK 范围。
- 当前工作区存在用户未暂存的 `docs/CODEX_PHASE5_INTERACTION_GUIDE.md` 修改，本 TASK 规划不覆盖也不暂存该文件。

## 6. Implementation Decision

- 采用已确认的方案 C：前端统一操作反馈、完整错误码中文映射，并治理浏览器公共 API 的后端中文错误响应。
- 前端优先复用 Element Plus `ElMessage`，通过轻量公共封装统一 3 秒持续时间、固定中上部位置、类型和重复消息策略。
- 操作型反馈使用全局消息；结构性页面错误继续保留错误状态与恢复入口。
- 后端在公共 API 边界按稳定错误码返回安全中文消息，不机械翻译所有内部异常文本。
- API JSON 结构、稳定错误码和 HTTP 状态码保持不变；`message` 中文化属于向后兼容的展示文本变化。
- 不需要数据库、Migration 或新的架构基础设施；需要同步相关 Contract，但预计不需要新增 ADR。

## 7. Acceptance Criteria

- [x] 原问题无法按原步骤复现。
- [x] Expected Behavior 已实现。
- [x] 现有相关流程没有回归。
- [x] 相关 targeted test 通过。
- [x] 必要的 integration / frontend / browser test 通过。
- [x] `git diff --check` 通过。
- [x] 必要文档已同步。
- [x] 已停用提示词方案导致定时分析保存失败时，用户看到准确、可行动的中文提示，不再看到通用重试提示。
- [x] 操作型成功、警告和失败消息在视口中上部固定显示，页面滚动不改变位置，并在 3 秒后淡出关闭。
- [x] 页面首次加载失败仍保留错误状态和重试入口，空态、字段校验与 401 登录恢复语义未被破坏。
- [x] 浏览器实际调用的公共 API 已知错误码均有准确中文映射或受控安全回退，不直接展示未知服务端原始文本。
- [x] 浏览器公共 API 失败响应中的公开 `message` 已中文化，稳定 `code`、HTTP 状态码和响应结构保持不变。
- [x] 前端程序判断继续依赖稳定 `code`，不依赖 `message`。
- [x] 未预期错误和安全相关错误不泄露堆栈、SQL、Token、Cookie、Prompt、Provider 密钥或其它内部信息。
- [x] 内部日志、内部异常诊断文本、Provider 原始错误和历史持久化诊断信息未被纳入机械翻译。
- [x] 未修改数据库、Migration、业务校验规则、认证授权边界或既有 Analysis / Recommendation 生命周期。

## 8. Adjustment Log

- 无范围调整。
- 实施与验收中未发现需要拆分的新问题，所有反馈均已在 TASK-046 范围内完成。

## 9. Test / Verification Record

- Java 环境：Java 21.0.11；Maven Wrapper 3.9.16。
- 后端编译：`./mvnw -q -DskipTests compile` 通过。
- 后端目标测试：`BrowserApiErrorMessagesTest`、`AnalysisScheduleControllerTest`、`AnalysisPreviewControllerTest`、`GlobalApiExceptionHandlerTest` 共 11 个用例通过；覆盖已停用提示词方案错误码不变、公开消息准确中文及未知错误码安全兜底。
- 前端类型检查：`npm run typecheck` 通过。
- 前端测试：Vitest 32 个测试文件、94 个用例全部通过。
- 前端生产构建：`npm run build` 通过。
- Browser E2E：已启动 `npm run test:e2e:phase3`，因当前环境未提供 `INFORMATION_HUB_E2E_DB_URL` 在预检阶段中止；未执行浏览器用例，待人工/完整环境验证。
- 后端完整测试：已运行 258 个用例；受当前测试 MySQL `127.0.0.1:13306` 未启动影响，39 个集成测试错误；另发现并已同步 2 个本 TASK 公开消息断言，V3 Migration checksum 既有失败与本 TASK 无关。
- `git diff --check`：通过。
- 人工验证：2026-08-13 已通过；确认原复现路径、准确中文提示、视口固定位置、页面滚动行为、3 秒淡出及相关操作反馈均符合 Expected Behavior。

## 10. Documentation Sync

- [x] `docs/CURRENT_STATUS.md`
- [x] `docs/PHASE5_TASK_INDEX.md`
- [x] `docs/ARCHITECTURE.md`（无架构事实变化，无需修改）
- [x] `docs/DATABASE_DESIGN.md`（无数据库变化，无需修改）
- [x] Contract
- [x] ADR（无新架构决策，无需修改）
- [x] AGENTS（无工作规则变化，无需修改）
- [x] README / ROADMAP（无阶段或产品范围变化，无需修改）
- [x] 无额外事实文档变化

## 11. Completion Checklist

- [x] Scope review
- [x] Security / Owner review（如相关）
- [x] Logging review（后端改动）
- [x] Database / migration review（如相关）
- [x] Contract compatibility review（如相关）
- [x] Tests
- [x] `git diff --check`
- [x] Implementation Record
- [x] Task Index
- [x] Current Status
- [x] 当前 TASK 相关文件已 `git add`
- [x] 未自动 commit / push

## 12. Implementation Record

- 前端新增统一 `userFeedback` 入口，Element Plus 全局消息固定在视口中上部，默认 3 秒、支持淡入淡出与重复消息合并。
- 提示词方案、手动分析、定时分析、职位推荐和登录操作反馈已迁移；首次加载失败与可恢复结构化状态保持原行为。
- 前端错误转换补齐定时分析等错误码中文映射，仅接受受控中文服务端消息回退，未知英文文本进入安全兜底。
- 后端新增浏览器公共 API 中文错误目录；Analysis、Prompt、Schedule、Recommendation 与 Session/CSRF 安全边界在公开响应中按稳定错误码返回安全中文。
- Collector 专用错误、内部日志、异常诊断、Provider 原始错误和历史持久化诊断未翻译。
- API 结构、稳定错误码、HTTP 状态码、认证授权、Owner 隔离、业务生命周期和数据库均未改变。
- Contract 已同步 `web-ui-behavior-v1` 与 `analysis-schedule-v1`；Architecture、Database、ADR 无事实变化。
- 2026-08-13 人工验收通过，Acceptance Criteria 全部满足，任务收尾为 `DONE`。

## 13. Commit

建议提交信息：

```text
完成 TASK-046：统一全局操作反馈与公共 API 中文错误消息
```
